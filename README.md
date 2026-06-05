# Offline Facial Recognition & Liveness Detection System for Datalake 3.0

> Hackathon 7.0 Submission – Mobile-Based Secure Offline Facial Recognition and Liveness Detection System

## Overview

This project is a lightweight, fully offline facial recognition and liveness detection solution designed for integration with the Datalake 3.0 ecosystem. The system enables secure authentication of field personnel operating in remote and zero-network environments using only standard mid-range mobile devices.

The solution combines facial recognition, offline liveness verification, and local data storage to ensure secure access control even without internet connectivity. Once connectivity is restored, authentication records can be synchronized with a central server and removed from local storage.

---

## Problem Statement

Develop a highly accurate, lightweight, and entirely offline facial recognition and liveness detection algorithm that can be seamlessly integrated into the existing Datalake 3.0 application, ensuring uninterrupted operations in zero-network zones.

---

## Key Features

### Offline Face Recognition

* Works without internet connectivity
* Generates facial embeddings locally
* Fast identity verification
* Supports multiple enrolled personnel

### Offline Liveness Detection

* Head movement challenge-response verification


### Lightweight Architecture

* Optimized for mobile deployment
* Designed for mid-range Android and iOS devices
* Low memory footprint
* Fast inference (<1 second target)

### Local Data Storage

* Stores authentication logs offline
* Works in zero-network zones
* Maintains verification history

### Sync & Purge Mechanism

* Uploads records when connectivity returns
* Removes synchronized records from local storage
* Supports future AWS integration

---

## System Architecture

```text
Camera Feed
     │
     ▼
Face Detection
     │
     ▼
Face Recognition
     │
     ▼
Identity Verification
     │
     ▼
Liveness Challenge
     │
     ▼
Pose Verification
     │
     ▼
Authentication Decision
     │
     ▼
Local Storage
     │
     ▼
Cloud Sync (When Available)
```

---

## Technology Stack

### AI & Computer Vision

* Python
* InsightFace
* ArcFace Embeddings
* OpenCV
* NumPy
* ONNX Runtime

### Mobile Application

* Expo
*  kotlin

### Storage

* SQLite
* Local Device Storage

### Backend (Future Scope)

* FastAPI
* AWS API Gateway
* AWS Lambda
* DynamoDB

---

## Project Workflow

### Enrollment

```text
Capture User Images
        ↓
Face Detection
        ↓
Embedding Generation
        ↓
Store Embeddings
```

### Authentication

```text
Capture Face
      ↓
Generate Embedding
      ↓
Compare With Database
      ↓
Identity Recognition
      ↓
Generate Challenge
      ↓
Verify Head Movement
      ↓
Authentication Success
```


---

## Repository Structure

```text
offline-face-authentication


│   ├── .idea
│   ├── app
│   ├── gradle
│   ├── NHAI_yolo.ipynb
│   ├── build.grade.kts
│   ├── gradle.properties
│   └── gradlew
│   └── gradlew.bat
│   └── settings.gradle.kts
|   

```

---

## Performance Goals

| Metric               | Target               |
| -------------------- | -------------------- |
| Recognition Accuracy | >95%                 |
| Authentication Time  | <1 sec               |
| Offline Capability   | 100%                 |
| Supported Devices    | Android 8+ / iOS 12+ |
| RAM Requirement      | ≥3 GB                |
| Model Size           | ~20 MB               |

---

## Future Enhancements

* Blink Detection
* Smile Detection
* MobileFaceNet Integration
* ONNX Runtime Mobile Deployment
* Multi-Factor Authentication
* Geotagged Verification
* Attendance Management
* Advanced Anti-Spoofing Models

---

## Use Cases

### NHAI Field Personnel Verification

* Highway inspectors
* Maintenance staff
* Toll operators
* Emergency response teams

### Offline Attendance Systems

* Construction sites
* Remote offices
* Field operations

### Secure Access Control

* Restricted facilities
* Government infrastructure
* Remote project sites

---

## Demo Workflow

```text
Open App
    ↓
Face Recognized
    ↓
Random Challenge Generated
    ↓
User Performs Challenge
    ↓
Pose Verification
    ↓
Authentication Success
    ↓
Record Stored Offline
    ↓
Sync When Network Returns
```

---

## Contributors

Developed as part of **Hackathon 7.0** for building a secure, lightweight, and offline-first facial authentication system compatible with Datalake 3.0.

---

## License

This project uses only open-source technologies and is intended for educational, research, and hackathon purposes.
