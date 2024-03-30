# Installation

All you need you do is drop the netsuite.metabase-driver.jar (from the Assets of the latest release https://github.com/ericcj/metabase-netsuite-driver/releases/), netsuite JDBC NQjc.jar from https://docs.oracle.com/en/cloud/saas/netsuite/ns-online-help/section_3994742720.html and oracle ojdbc8.jar (see https://www.metabase.com/docs/latest/databases/connections/oracle even if though it's unused just because we inherit that driver) in your `/path/to/metabase/plugins/` directory.

# Why

https://github.com/metabase/metabase/issues/26251

# Building the driver

## Prereq: Install the Clojure CLI

Make sure you have the `clojure` CLI version `1.10.3.933` or newer installed; you can check this with `clojure
--version`. Follow the instructions at https://clojure.org/guides/getting_started if you need to install a
newer version.

## Prereq: Download JDBC jar

You need \~/NetSuiteJDBCDrivers/NQjc.jar from above

## Prereq: Configure paths in `~/.clojure/deps.edn`

See https://github.com/metabase/sudoku-driver/blob/master/README.md#hacking-on-the-driver-locally

## Build it


```sh
# Example for building the driver with bash or similar

# switch to the local checkout of the Metabase repo
cd /path/to/metabase/repo

# to create metabase/resources/modules/oracle.metabase-driver.jar
bin/build-drivers.sh

# get absolute path to the driver project directory
DRIVER_PATH=`readlink -f ~/metabase-netsuite-driver`

# Build driver. See explanation at https://github.com/metabase/sudoku-driver/blob/master/README.md#build-it-updated-for-build-script-changes-in-metabase-0460
clojure \
  -Sdeps "{:aliases {:netsuite {:extra-deps {com.metabase/netsuite-driver {:local/root \"$DRIVER_PATH\"}}}}}"  \
  -X:build:netsuite \
  build-drivers.build-driver/build-driver! \
  "{:driver :netsuite, :project-dir \"$DRIVER_PATH\", :target-dir \"$DRIVER_PATH/target\"}"
```

will create `target/netsuite.metabase-driver.jar`. Copy this file and NQjc.jar to `/path/to/metabase/plugins/` and restart your
server, and the driver will show up.

## Testing

Sample driver stuff might work https://github.com/metabase/sample-driver#interactive-testing

e.g. this does work:

MB_NETSUITE_DRIVER_TEST_PLUGIN_MANIFEST_PATH=/path/to/metabase-netsuite-driver/resources/metabase-plugin.yaml clojure -M:dev:ee:ee-dev:drivers:drivers-dev:user:trace:deps-alpha:user/metabase-netsuite-driver:nrepl
