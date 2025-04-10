# UCS Location Synchronization Project
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/1f5753fc9732488c87a21d544cd441aa)](https://app.codacy.com/gh/Digital-Square-Tanzania/hfr-ucs-location-sync-service/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
![Java CI with Gradle](https://github.com/Digital-Square-Tanzania/hfr-ucs-location-sync-service/actions/workflows/gradle.yml/badge.svg)
[![Coverage Status](https://coveralls.io/repos/github/Digital-Square-Tanzania/hfr-ucs-location-sync-service/badge.svg)](https://coveralls.io/github/Digital-Square-Tanzania/hfr-ucs-location-sync-service)

A Java-based application designed to synchronize health facility location data between the OpenMRS and Health Facility Registry (HFR) systems. It also supports importing hamlet-level location data from CSV files to ensure comprehensive location data management for healthcare systems.

## Overview

The UCS Location Synchronization Project performs several key functions:

- **Administrative Hierarchy Processing:** Retrieves and processes hierarchical data from HFR to maintain proper parent-child relationships between locations.
- **CSV Import:** Imports hamlet location data from CSV files for fine-grained location management.
- **Robust Error Handling:** Implements retry mechanisms and detailed logging to address network issues or data inconsistencies.

## Features

- **OpenMRS Integration:** Fetches location data from an OpenMRS instance.
- **HFR Data Synchronization:** Retrieves health facility and administrative hierarchy data from HFR with configurable retry attempts.
- **CSV Import:** Imports hamlet location data from CSV files.
- **Caching & Data Lookup:** Maintains caches for rapid lookups by location UUID and code.
- **Modular Architecture:** Organized into clearly separated packages such as `domain`, `service`, and `util`.
- **Configurable Settings:** Uses `application.conf` for environment-specific configuration.

## Technologies

- **Java**
- **Gradle**
- **Typesafe Config**
- **OkHttp & Apache HttpClient**
- **JSON (org.json)**
- **JUnit & Mockito**

## Project Structure

- **`Main.java`**: The entry point of the application.
- **`domain`**: Contains data model classes (e.g., `Location`, `User`, `LocationCSVRow`).
- **`service`**: Contains service classes (e.g., `HfrService`, `OpenmrsLocationService`) for data synchronization.
- **`util`**: Contains utility classes for HTTP requests, CSV parsing, configuration management, etc.
- **`src/main/resources`**: Contains configuration and CSV data files.

## Prerequisites

- Java JDK 8 or higher.
- Gradle build tool.

## Configuration

The application uses the Typesafe Config library. Update the `application.conf` file with your environment settings:

```hocon
openmrs {
  base_url = "http://127.0.0.1:8081/openmrs/"
  user = "openmrs_username"
  password = "openmrs_password"
  code_location_attribute_uuid = "d1ea5c1b-4c54-4695-af80-5c4c25bdcaf7"
  hfr_code_location_attribute_uuid = "8bcf11a9-920a-488d-9e3c-251f67e348b3"
}
hamlet {
  resource_file_name = "Hamlets.csv"
}
hfr {
  baseUrlGetHealthFacilities = "https://hfrs.moh.go.tz/web/index.php?r=api%2Fhealth-facility%2Fhealth-facility-list&search_query=all&page="
  baseUrlGetHierarchy = "https://hfrs.moh.go.tz/web/index.php?r=api/health-facility/administrative-hierarchy&page="
  username = "username"
  password = "password"
}
```

## Building and Running

### Building

Build the project using Gradle:

```bash
gradle build
```

### Running the Application

You have two options:

#### 1. Using Gradle

Run the application with:

```bash
gradle run
```

#### 2. Using the Executable JAR

Generate the executable JAR with:

```bash
gradle shadowJar
```

Then run the JAR with:

```bash
java -jar build/libs/ucs-location-synchronization-all.jar
```

## Testing

Run tests using:

```bash
gradle test
```

## Contributing

Contributions are welcome! Please fork this repository and submit pull requests, or open an issue for bug reports and feature requests.

## License

This project is licensed under the MIT License. See the LICENSE file for details.