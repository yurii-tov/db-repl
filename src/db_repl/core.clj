(ns db-repl.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.repl :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as cstr]
            [clojure.pprint :refer [print-table pprint pp]])
  (:import java.sql.Timestamp
           java.util.UUID
           (java.time Instant
                      Period)))


;;=====================
;; Connection machinery
;;=====================


(defn path-to-uri [path]
  (str "jdbc:firebirdsql://localhost:3050/"
       (cstr/replace (.getCanonicalPath (io/file path)) "\\" "/")
       "?lc_ctype=UTF8"))


(defn make-spec
  "Create new spec hashmap for given file using default user/password"
  [path-to-db]
  {:connection-uri (path-to-uri path-to-db)
   :user "sysdba"
   :password "masterkey"})


(def ^:dynamic *db-spec*
  "Global db spec to use implicitly in helper functions"
  (make-spec
   (str (System/getProperty "java.io.tmpdir") "ls.fdb")))


(defmacro with-connection-reuse [& body]
  `(jdbc/with-db-connection [~'conn *db-spec*]
     (binding [*db-spec* ~'conn]
       (do ~@body))))


;; ==========
;; inspect db
;; ==========


(defn query-pprint
  "Query db according to *db-spec*, display result using print-table
  Arguments is query vector or sql string"
  [q]
  (print-table
   (jdbc/query *db-spec*
               (if (vector? q) q (vector q))
               {:keywordize? false})))


(defn columns
  "Get list of columns for given table.
   Arguments:
   table - a lower-case keyword with table name
   Return list of keywords"
  [table]
  (sort (map (comp keyword
                   cstr/lower-case
                   cstr/trim
                   :rdb$field_name)
             (jdbc/query *db-spec*
                         ["select rdb$field_name
                           from rdb$relation_fields
                           where rdb$relation_name=?;"
                          (cstr/upper-case (name table))]))))


(defn tables
  "Get hashmap of all tables + columns"
  []
  (with-connection-reuse
    (into {}
          (map (fn [record]
                 (let [name (-> record
                                :rdb$relation_name
                                cstr/trim
                                cstr/lower-case
                                keyword)]
                   (vector name (columns name))))
               (jdbc/query *db-spec*
                           ["select rdb$relation_name
                             from rdb$relations
                             where rdb$view_blr is null
                             and (rdb$system_flag is null or rdb$system_flag = 0);"])))))


(defn find-table
  "Get list of tables matching given regexp"
  [re]
  (sort-by first
           (filter (comp (partial re-find re) name first)
                   (tables))))


;; =======
;; edit db
;; =======


(defn timestamp-shift-by-days
  "Shift given java.sql.Timestamp by amount of days"
  ([timestamp days]
   (Timestamp/from
    (.plus (.toInstant timestamp)
           (Period/ofDays days))))
  ([days]
   (timestamp-shift-by-days
    (Timestamp/from (Instant/now))
    days)))


(defn execute-sql [sql]
  "Execute raw chunk of sql, statement by statement (detect ; as separator)"
  (with-connection-reuse
    (doseq [line (cstr/split (cstr/replace sql "#;+$" "") #";\s*")]
      (printf "%s => %s\n"
              line
              (jdbc/execute! *db-spec* [line])))))


;; =====
;; utils
;; =====


(defn gen-uuid []
  (str (UUID/randomUUID)))
