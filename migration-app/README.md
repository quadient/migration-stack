# migration-app

## Running the app

Execute the `bin/run` script in the distribution directory to start the server. The server uses `application.conf` in
the same directory, runs in production mode and listens on http://localhost:21000 by default. Modules are loaded from
the `modules-dir` specified in `application.conf`.

## Running for development

Run the server from the repository root:

```
./gradlew :migration-app:runServer
```

The server uses `src/main/resources/application.conf`, runs in development mode and listens on
http://localhost:21000 by default. Modules are loaded directly from
`../migration-examples/src/main/groovy`, including the modules that are meant only for Gradle.

In development mode the server proxies all non-API requests to the Vite dev server on port 5173. Start it for hot
reload of the frontend:

```
cd migration-app/web
npm install
npm run dev
```

When the dev server is not running, the server falls back to the prebuilt frontend in `web/dist`.
If the requested file does not exist there, the server runs `npm run build` in `web` first.

## Configuration

Configuration is done via `application.conf`. All available options can be seen in the sample file provided.
Relative paths are resolved against the working directory.

## Modules

The app loads every `.groovy` file in `modules-dir` that starts with the module frontmatter and provides them
as a runnable task in the web UI. Only one module can run at a time.

## Script libraries

Modules can use additional libraries that are not bundled with the app. Put the `.jar` files into the `libs`
directory and they will be added to the classpath of every module run.

- The directory is resolved relative to the working directory and can be changed with the `libs-dir` key in
  `application.conf`.
- Only `.jar` files placed directly in the directory are loaded, subdirectories are ignored.
- The jars are loaded again for every module run, so libraries can be added or replaced without restarting the app.

## Use from the CLI

Run `./gradlew :migration-app:run -q --args="--help"` during development, or `bin/run --help`(linux) /
`bin/run.bat --help`(windows) from the distribution directory to see all available options for the CLI version of the
app.

## Building the distribution

Build the distribution from the repository root:

```
./gradlew :migration-app:buildDistZip
```