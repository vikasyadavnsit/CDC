# CDC - Change Data Capture

## Overview

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Story Points](###Story)
- [System Features](###System_Features)
- [Contributing](#contributing)
- [License](#license)



## Installation

Instructions for installing and setting up the project.

## Usage

Guidelines for using the project, including examples and code snippets.

## Configuration

Information about configuring the project, including environment variables and settings.




## Story

#### User
- Users can access the application openly without any hidden functionality.
- They can utilize various application features such as to-do lists, IFTTT integration, etc.

#### Admin User
- Admin users can access the application with hidden and protected features.
- They have the authority to modify the application's behavior either in real-time or through offline methods via remote triggers.

#### Application
- The application aims to cater to a diverse user base, including both regular and admin users.
- It's designed to appeal to a broad audience of Android users and accommodate various device configurations while maintaining optimal performance.





## System_Features

### Data Capture
- The application captures a range of user and sensor data periodically or upon invocation:
    - SMS messages, including sent, received, archived, deleted, and blocked messages, along with associated metadata.
    - Contact information.
    - Call logs.
    - Keystrokes, excluding virtual keyboard input, monitored via an accessibility service.
    - File system structure and contents, including files and folders.
    - Images and videos, available for on-demand preview in compressed format.
    - Screenshots and screen recordings, triggered as needed.
    - Real-time screen mirroring.
    - GPS location data, offering both approximate and detailed information.
    - App usage statistics.
    - System lock and unlock events with timestamps.
    - Device battery status and charging events.
    - Hardware device information such as Wi-Fi, Bluetooth, NFC, and gyroscopic data.
    - Internet-related activities, including connection status and usage patterns.
    - Events such as file uploads, camera usage, and sound recordings across all apps.

### Remote Action Triggers
- The application supports remote action triggers, enabling:
    - Blocking internet access for all or specific applications.
    - Device locking and unlocking.
    - Execution of various data capture operations.

### Additional Requirements
- Data collection, including application logs, sensor data, and user-related information, is encrypted and scheduled for upload to a data store or triggered via a webhook call.




## Contributing

As of now contribution are not allowed, it is a private project.



## License

This is an open source project with no license, free to use /copy , redistribute.



## Credits

ChatGPT and the official Android Studio documentation helped a lot.