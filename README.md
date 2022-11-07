All you need you do is drop the driver and netsuite JDBC NQjc.jar in your `/path/to/metabase/plugins/` directory.

## Building the driver

## Prereq: Install the Clojure CLI

Make sure you have the `clojure` CLI version `1.10.3.933` or newer installed; you can check this with `clojure
--version`. Follow the instructions at https://clojure.org/guides/getting_started if you need to install a
newer version.

## Prereq: Download JDBC jar

You need \~/NetSuiteJDBCDrivers/NQjc.jar from https://docs.oracle.com/en/cloud/saas/netsuite/ns-online-help/section_3994742720.html but also the oracle one even if though it's unused just because we inherit that driver

## Prereq: Configure paths in `~/.clojure/deps.edn`

See https://github.com/metabase/sudoku-driver/blob/master/README.md#hacking-on-the-driver-locally

## Build it

```sh
clojure -X:build :project-dir "\"$(pwd)\""
```

will create `target/netsuite.metabase-driver.jar`. Copy this file and NQjc.jar to `/path/to/metabase/plugins/` and restart your
server, and the driver will show up.

## Testing

Sample driver stuff might work https://github.com/metabase/sample-driver#interactive-testing

e.g. this does work:

MB_NETSUITE_DRIVER_TEST_PLUGIN_MANIFEST_PATH=/path/to/metabase-netsuite-driver/resources/metabase-plugin.yaml clojure -M:dev:ee:ee-dev:drivers:drivers-dev:user:trace:deps-alpha:user/metabase-netsuite-driver:nrepl
