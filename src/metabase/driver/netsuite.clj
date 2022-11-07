(ns metabase.driver.netsuite
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [honeysql.core :as hsql]
            [honeysql.format :as hformat]
            [java-time :as t]
            [metabase.config :as config]
            [metabase.driver :as driver]
            [metabase.driver.common :as driver.common]
            [metabase.driver.impl :as driver.impl]
            [metabase.driver.sql :as sql]
            [metabase.driver.sql-jdbc.common :as sql-jdbc.common]
            [metabase.driver.sql-jdbc.connection :as sql-jdbc.conn]
            [metabase.driver.sql-jdbc.execute :as sql-jdbc.execute]
            [metabase.driver.sql-jdbc.sync :as sql-jdbc.sync]
            [metabase.driver.sql.query-processor :as sql.qp]
            [metabase.driver.sql.util :as sql.u]
            [metabase.driver.sql.util.unprepare :as unprepare]
            [metabase.models.secret :as secret]
            [metabase.util :as u]
            [metabase.util.honeysql-extensions :as hx]
            [metabase.util.i18n :refer [trs]])
  (:import [java.sql Connection ResultSet Types]
           [java.time Instant OffsetDateTime ZonedDateTime]))

(driver/register! :netsuite, :parent :oracle)

(defn- netsuite-spec [details spec host port account-id role-id]
  (-> (assoc spec :subname (str "//" host
                            ":" port
                            ";ServerDataSource=NetSuite2.com;encrypted=1;CustomProperties=(AccountID=" account-id
                            ";RoleID=" role-id ");NegotiateSSLClose=false"))
      (sql-jdbc.common/handle-additional-options details)))

(defmethod sql-jdbc.conn/connection-details->spec :netsuite
  [_ {:keys [host port account-id role-id]
      :or   {host "localhost", port 1708}
      :as   details}]
  (let [spec      {:classname "com.netsuite.jdbc.openaccess.OpenAccessDriver", :subprotocol "ns"}
        finish-fn (partial netsuite-spec details)]
    (-> (merge spec details)
        (dissoc :host :port :account-id :role-id)
        (finish-fn host port account-id role-id))))

(defmethod driver/can-connect? :netsuite
  [driver details]
  (sql-jdbc.conn/can-connect? driver details))

