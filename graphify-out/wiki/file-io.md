# File I/O — Local Storage Pipeline

CDC writes captured data to encrypted local files on external storage (`/sdcard/Documents/CDC/`) and maintains a local Room database. Two appender classes handle different write patterns: `CDCOrganisedFileAppender` for structured, deduplication-checked record data, and `CDCUnorganisedFileAppender` for high-frequency streaming data. `CDCFileReader` decrypts and reads files back. `CryptoUtils` provides the AES-256/CBC layer used by both appenders.

## Key Classes

| Class | Responsibility |
|-------|---------------|
| `FileUtils` | High-level dispatcher — routes a write call to the correct appender based on `FileMap.isOrganized` |
| `CDCOrganisedFileAppender` | Writes `List<Map<String,String>>` records with optional deduplication by unique ID; supports per-line encryption |
| `CDCUnorganisedFileAppender` | Singleton with per-FileMap in-memory queues; flushes to disk when buffer exceeds byte limit |
| `CDCFileReader` | Reads a `FileMap` file line-by-line; decrypts encrypted lines; can write to `TEMPORARY_FILE` |
| `CryptoUtils` | AES-256/CBC encrypt/decrypt; Base64 encoding/decoding; key and IV helpers |
| `FileMap` | Enum mapping logical data type → directory path, file name, encryption flag, deduplication key |
| `AppContext` | Application-level context holder used by services |

---

## File Locations

All files live under `Environment.getExternalStoragePublicDirectory("Documents/CDC")`, which resolves to `/sdcard/Documents/CDC/` on most devices.

| FileMap | File | Encrypted | Organised | Dedup key |
|---------|------|-----------|-----------|-----------|
| `SMS` | `sms.txt` | yes | yes | `_id` |
| `CALL` | `call.txt` | yes | yes | `_id` |
| `CONTACTS` | `contacts.txt` | yes | yes | `_id` |
| `CALL_STATE` | `call_state.txt` | yes | no | — |
| `KEYSTROKE` | `keystroke.txt` | yes | no | — |
| `LOG` | `log.txt` | no | no | — |
| `APPLICATION_USAGE` | `application_usage.text` | no | no | — |
| `NOTIFICATION` | `notification.txt` | no | no | — |
| `DIRECTORY_STRUCTURE` | `directory_structure.txt` | no | no | — |
| `TEMPORARY_FILE` | `temp.txt` | no | no | — |

Sensor files are written separately by `CDCUnorganisedFileAppender.appendDataToFile()` to `Documents/CDC/Sensors/<SensorName>.txt`.

The Room database (`cdc.db`) is stored at `Documents/CDC/db/cdc.db`.

---

## FileUtils (routing layer)

`FileUtils.appendDataToFile(FileMap fileMap, Object data)` dispatches:

```
if fileMap.isOrganized()
    → CDCOrganisedFileAppender.checkAndAppendDataInOrganizedFile(fileMap, data)
else
    → CDCUnorganisedFileAppender.add(fileMap, data)
```

`FileUtils.startFileAccessSettings(Activity)` opens the system "All files access" settings screen so the user can grant `MANAGE_EXTERNAL_STORAGE`.

---

## CDCOrganisedFileAppender

Designed for structured record data (SMS, contacts, call logs) where each row has a unique ID that should not be duplicated in the file.

### Write flow

```
checkAndAppendDataInOrganizedFile(FileMap, Object data)
  │
  ├── resolve File from Environment.getExternalStoragePublicDirectory(fileMap.directoryPath)
  ├── checkAndCreateDirectory() / checkAndCreateFile() if missing
  │
  ├── if fileMap.checkForDuplication
  │     └── getAllUniqueIdsInFile()
  │           ├── reads every line from the existing file
  │           ├── decrypts if fileMap.isEncrypted
  │           └── collects values of lines starting with "<uniqueIdKey>:"
  │
  └── appendToBufferWriterInOrganizedFile()
        │ (data is List<Map<String,String>>)
        ├── for each record:
        │     ├── writes "Record:<n> DateTime:<now>" header line (encrypted)
        │     ├── skips record if its unique ID already in file
        │     └── writes "key: value" lines (each individually encrypted)
        └── flushes BufferedWriter
```

### Encryption in organised mode

Each individual line is encrypted before writing:

```java
CryptoUtils.getEncryptedData(fileMap, "key: value")
  // if fileMap.isEncrypted && Android >= O:
  //   AES-256/CBC encrypt → Base64 encode
  // else:
  //   return plaintext
```

---

## CDCUnorganisedFileAppender

Singleton with per-`FileMap` in-memory `Queue<String>` buffers. Designed for high-frequency writes (sensor events, keystrokes) where deduplication is not needed.

### Buffer flush policy

```
add(FileMap, Object data)
  ├── computes byte size of data.toString()
  ├── if currentByteCount + dataBytes > byteLimit (default: 64 bytes from AppConstants)
  │     └── writeToFile(fileMap)   ← flush queue to disk
  ├── enqueue data string
  └── update currentByteCount
```

Note: the default `FILE_APPENDER_BUFFER_SIZE` is `64` bytes, which is very small and will cause frequent flushes for any data larger than a few words.

### Write-to-file

```
writeToFile(FileMap)
  ├── resolve and create directory/file if needed
  └── drain queue:
        for each item in queue:
          write: CryptoUtils.getEncryptedData(fileMap, LocalDateTime.now() + " :: " + item) + "\n"
        reset byteCount to 0
```

Uses `BufferedWriter` with UTF-8 charset (Android 13+) or default charset (older versions).

### Direct sensor writes

`appendDataToFile(String fileName, String data)` is a separate static method that bypasses the queue entirely and writes sensor readings straight to `Documents/CDC/Sensors/<fileName>`. Used by `CDCSensorService.onSensorChanged()`.

---

## CDCFileReader

Reads a `FileMap` file back from disk, optionally decrypting each line and/or writing decrypted content to `FileMap.TEMPORARY_FILE`.

### Read modes

| Method | print to log | write to temp |
|--------|-------------|---------------|
| `readAndCreateTemporaryFile(fileMap)` | no | yes |
| `readAndPrint(fileMap)` | yes | no |
| `readAndPrintAndWrite(fileMap)` | yes | yes |

### Decryption

```java
decryptIfNeeded(String line)
  // Base64 decode → AES-256/CBC decrypt → UTF-8 string
  // Uses AppConstants.CRYPTO_AES_SECRET_KEY and CRYTPO_AES_IV
  // Returns original line on any exception
```

Before reading, `readAndProcess()` deletes any existing `TEMPORARY_FILE` to avoid stale data accumulation.

---

## CryptoUtils

Pure utility class. Uses a **fixed** AES-256/CBC key and IV stored in `AppConstants`:

```
CRYPTO_AES_SECRET_KEY = "lgr2sIXLgsywblgrNvqI0m7FJfcJZon6xKQ0ixhPbJw="  (Base64)
CRYTPO_AES_IV         = "suXmtxjpt3IQtIYUtb3/3A=="                       (Base64)
```

| Method | Purpose |
|--------|---------|
| `encrypt(SecretKey, iv, byte[])` | AES/CBC/PKCS5Padding encrypt |
| `decrypt(SecretKey, iv, byte[])` | AES/CBC/PKCS5Padding decrypt |
| `generateKey()` | Generates a new random 256-bit AES key (used only in `keyGenerationOneTime()`) |
| `generateIV()` | Generates a random 16-byte IV |
| `stringToKey(String)` | Base64 decode → `SecretKeySpec` |
| `stringToIV(String)` | Base64 decode → `byte[]` |
| `keyToString(SecretKey)` | `SecretKey.getEncoded()` → Base64 |
| `ivToString(byte[])` | `byte[]` → Base64 |
| `getEncryptedData(FileMap, String)` | Convenience: encrypts only if `fileMap.isEncrypted && API >= O` |

Security note: the key and IV are hardcoded constants. All devices share the same encryption key, so the encrypted files can be decrypted by anyone with the APK source.

---

## AppContext

An `Application`-level singleton that holds a static reference to the application `Context`. Accessed by services and utilities that cannot receive a `Context` through normal dependency injection.

---

## Complete Write Path Example (SMS capture)

```
ClickActions.CAPTURE_ALL_SMS triggered
        │
MessageUtils.getMessages(context, FileMap.SMS)
        │ → List<Map<String,String>> (cursor rows from Telephony.Sms)
        │
FileUtils.appendDataToFile(FileMap.SMS, messageList)
        │ SMS.isOrganized = true
        ▼
CDCOrganisedFileAppender.checkAndAppendDataInOrganizedFile(FileMap.SMS, messageList)
        │
        ├── getAllUniqueIdsInFile()  — reads existing sms.txt, collects _id values
        │
        └── for each SMS record not already in file:
              ├── write encrypted: "Record:1 DateTime:2026-05-17T..."
              └── write encrypted: "_id: 42\naddress: +91...\nbody: Hello\n..."
```

```
FirebaseUtils.uploadUserSmsDataSnapshot(messageList)
        │
        └── for each message:
              RTDB: cdc/users/<id>/userDeviceData/sms/<_id> = { map }
```