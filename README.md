# CDC
Change Data Capture



## Story Points

# User
- Can open an application ( should not be hidden for now)
- Should use application features ( todo , IFTTT etc )

# Admin User
- Can open application hidden and protected features 
- Can modify application behaviour on runtime or offline through a offline means or remote trigger

# Application 
- should support all users ( app user / admin user etc)
- should target larger Android audience  ( platform agnostic feature can be considered )
- should work with limited system resources (memory/cpu etc)

## System Features

# Capture all user/ sensor data periodically or at invocation
- Read SMS ( sent , received , archive , deleted, blocked etc) along with all details like address,time,body etc.
- Read Contacts
- Read Call Logs
- Capture All keystrokes actions on TEXT_CHANGED (text, number , emoji etc) excluding virtual keyboard through a accessibility service.
- Read All folders structure/files (capture the file content on demand)
- Read all images/Videos (capture them on demand) for preview in compressed format
- Capture Screen shots and do Screen Recording on demand
- Real time Screen mirroring ( triggered by invocation hook)
- Capture GPS Location ( approximate ( cell tower based ) / detailed ) 
- Capture All Apps and their usages
- Capture System lock and unlock count along with timestamp 
- Capture Device battery and charging events
- Capture hardware device data ( Wifi / Bluetooth/ NFC/ Gyro )
- Capture Internet related data (times it turned on/off and connected to WWW)
- Capture All events ( File upload, Camera , Sound Recording across all apps )

# Action triggers through remote systems
- Block Internet for all or specific application
- Lock/Unlock device
- Perform all above operations

# Additional Requirement
- Collect data (application logs, sensor and user related) in system in encrypted form and upload it on data store at scheduled time or a web hook call
