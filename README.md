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

## Statements of Contribution:
### Sebastian Veijalainen:
- Initial implementation of `cloneRepo` (this was revised later)
- Implementation of `analyzeResults` (parsing XML files containing test results)
- Tests for `analyzeResults`
- Tests for `build`
- Parts of `handle` method (collaboratory)
- Code reviews

### Jonas Sävås:
- Gradle setup with GitHub Actions
- `cleanup` method to remove build-directory and target-directory
- Tests for `cleanup` build/target
- Tests for git functions (updating/cloning)
- Small contribution in the `handle` method (collaboratory)
- Code Review

### Samuel Falk
- Setup server on a raspberry pi
- Implementation of `postStatus` (create commit status)
- Implementatin of git features
- Tests for `postStatus`
- Parts of `handle` (Collaboratory)
- Structuring of server config (`config.json`)
- Code reviews

### Simon Blom
- Implemented the `build` function.
- Worked on the functionality of logging build history.

## Analyzis of Team (SEMAT)
We would say that we fulfill most of the applicable points in the checklist for the “Seeded” category, and to some extent the “Formed” category, and would therefore put ourselves in between these two. We have reached a relatively productive state where all members can operate individually in parallel. However, everything is still not 100% clear and we have continuous discussion about various ways of organizing the work/workflow to be more productive. 