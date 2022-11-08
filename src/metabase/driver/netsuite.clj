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

(def ^:private database-type->base-type
  (sql-jdbc.sync/pattern-based-database-type->base-type
   [[#"BIGINT"     :type/BigInteger]]))

(defmethod sql-jdbc.sync/database-type->base-type :netsuite
  [driver column-type]
  (or (database-type->base-type column-type)
      ((get-method sql-jdbc.sync/database-type->base-type :oracle) driver column-type)))

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

; TIMESTAMP columns (e.g. item.createddate) were causing "Receiver class com.netsuite.jdbc.oabase.oacb does not define or inherit an implementation of the resolved method 'abstract java.lang.Object getObject(int, java.lang.Class)' of interface java.sql.ResultSet"
; maybe this is a similar concern to how the oracle driver handles TIMESTAMPTZ?
(defmethod sql-jdbc.execute/read-column-thunk [:netsuite Types/TIMESTAMP]
  [_ ^ResultSet rs _rsmeta ^Integer i]
  (fn []
    (when-let [t (.getTimestamp rs i)]
      (t/zoned-date-time (t/local-date-time t) (t/zone-id "UTC")))))

; Hack since SuiteQL doesn't allow quoting of any kind around aliases.
; Ideally we'd extend quote-style to support :none or something which then passed :quoted false to hsql/format instead of always passing :quoting as we do now: https://github.com/seancorfield/honeysql#entity-names
; This is an incomplete solution since you can't use quotes to refer to a subquery's alias either, e.g. you need to unquote "bar" to make this work: select "source"."bar" from (select * from baz) source
(defmethod driver/mbql->native :netsuite
  [driver outer-query]
  (let [parent-method (get-method driver/mbql->native :oracle)
        compiled      (parent-method driver outer-query)]
    (assoc compiled :query (str/replace (str/replace (str/replace (str/replace (str/replace (compiled :query) #" AS \"([^\"]+)\"" " AS $1") #"\"([^\"]+)\" AS " "$1 AS ")
    		; pretty gross to have special-handling for keywords just because of the netsuite "Task"."order" column here, especially avoiding "source".order for limit subquery
      #"([^e])\"\.order" "$1\".\"order\"") #"AS order([, ])" "AS esc_order$1") #"\.order" ".esc_order"))))
