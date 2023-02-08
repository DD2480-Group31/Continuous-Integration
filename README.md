# A Custom Continuous-Integration Server Written in Java

## Setup/Requirements
- Your project needs to use Gradle.
- The server is designed to be ran on a linux system using bash.
- When recieving a request, the server will run the standard `./gradlew build` command from the project root.
  - Test results are expected to be generated in the `build/test-results/test` directory, e.i. Gradle's default directory for test results.
- You need to setup a `config.json` file in the project root that contains the following fields:
  - `port` - The port of the server.
  - `main-branch` - The main branch of the project repository (usually "master", or "main").
  - `status-token` - A Github access token with permissions to create commit statuses on the repository.

## Running the Server
After following the setup steps/requirements above, the server can be run by running `./gradlew run` from the project root. This will start a server on the configured `port` which can then be forwarded by [ngrok](https://ngrok.com/) or some other hosting software.