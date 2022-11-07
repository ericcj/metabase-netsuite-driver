(ns metabase.driver.netsuite-test
  "Tests for specific behavior of the Netsuite driver."
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [honeysql.core :as hsql]
            [metabase.api.common :as api]
            [metabase.driver :as driver]
            [metabase.driver.netsuite :as netsuite]
            [metabase.driver.sql-jdbc.connection :as sql-jdbc.conn]
            [metabase.driver.sql-jdbc.sync :as sql-jdbc.sync]
            [metabase.driver.sql.query-processor :as sql.qp]
            [metabase.driver.util :as driver.u]
            [metabase.models.database :refer [Database]]
            [metabase.models.field :refer [Field]]
            [metabase.models.table :refer [Table]]
            [metabase.public-settings.premium-features :as premium-features]
            [metabase.query-processor :as qp]
            [metabase.query-processor-test :as qp.test]
            [metabase.query-processor-test.order-by-test :as qp-test.order-by-test] ; used for one SSL connectivity test
            [metabase.sync :as sync]
            metabase.sync.util
            [metabase.test :as mt]
            [metabase.test.data.interface :as tx]
            [metabase.test.data.netsuite :as netsuite.tx]
            [metabase.test.data.sql :as sql.tx]
            [metabase.test.data.sql.ddl :as ddl]
            [metabase.test.util :as tu]
            [metabase.util :as u]
            [metabase.util.honeysql-extensions :as hx]
            [toucan.db :as db]
            [toucan.util.test :as tt])
  (:import java.util.Base64))

(deftest connection-details->spec-test
  (doseq [[^String message expected-spec details]
          [["You should be able to connect"
            {:classname   "com.netsuite.jdbc.openaccess.OpenAccessDriver"
             :subprotocol "ns"
             :subname     "//1.2.3.4:5678;ServerDataSource=NetSuite2.com;encrypted=1;CustomProperties=(AccountID=910;RoleID=1112);NegotiateSSLClose=false"}
            {:host "1.2.3.4"
             :port 5678
             :account-id  910
             :role-id 1112}]
             ]]
    (let [actual-spec (sql-jdbc.conn/connection-details->spec :netsuite details)]
      (is (= (dissoc expected-spec)
             (dissoc actual-spec))
          message))))

