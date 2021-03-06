(ns db-repl.oko
  (:require [clojure.string :as cstr]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [db-repl.core :as db-repl]
            [clojure.java.jdbc :as jdbc]))


(defn terms []
  (db-repl/query-pprint "select name, date_start, date_finish, term_type, term_num from rterm;"))


;; Municipalitet


(defn schools []
  (db-repl/query-pprint "select id,registration_id,name,contact_name,contact_email from school;"))


(defn school-aggregated-statistic []
  (db-repl/query-pprint
   "select installation_id as iid,
    term_name as name,
    term_start,
    term_finish as finish,
    term_num as num,
    term_type as type,
    term_year
    from school_agregated_statistic;"))


(defn rubrics []
  (db-repl/query-pprint
   "select r.rubric, s.name as subject from rsubject as s join rsubjectrubrics as r on s.id = r.subjectid;"))


;; Working with JSON payload


(defn read-term-json
  "Read and parse OKO term payload from json file"
  [file-path]
  (with-open [r (io/reader file-path)]
    (json/read-json r)))


(defn payload-classes-results
  "Get flattened list of all classes results"
  [payload]
  (->> payload
       :classesResults
       :parallelsResults
       (mapcat (comp vals
                     :classesResults
                     second))))


;; Generate testdata


(defn read-schools-from-csv
  "Gen jdbc-compatible schools records from given csv file.

  Intended to use with insert command:
  (jdbc/insert-multi! *db-spec*
                      :school
                      (read-schools-fromp-csv
                       \"schools.csv\"))
  IMPORTANT NOTE:
  Expect csv file with following header:
  installation_id;name;contact_name;contact_email"
  [schools-csv & {:keys [id-from] :or {id-from 0}}]
  (let [id (atom id-from)
        gen-id (fn [] (swap! id inc))]
    (with-open [r (io/reader schools-csv)]
      (vec (for [line (drop 1 (line-seq r))]
             (assoc
              (zipmap [:installation_id :name :contact_name :contact_email]
                      (cstr/split line #";"))
              :id (gen-id)
              :guid (db-repl/gen-uuid)))))))


(defn insert-schools
  [csv-file & options]
  (let [schools (apply read-schools-from-csv
                       (cons csv-file options))]
    (db-repl/with-connection-reuse
      (doseq [{:keys [guid
                      installation_id
                      name
                      contact_name
                      contact_email]} schools]
        (jdbc/execute!
         db-repl/*db-spec*
         ["insert into school
           (id, guid, registration_id, name, contact_name, contact_email) values
           (gen_id(gen_schools, 1), ?, ?, ?, ?, ?);"
          guid installation_id name contact_name contact_email])))))


(defn insert-subjects
  [subjects-csv]
  (db-repl/with-connection-reuse
    (with-open [r (io/reader subjects-csv)]
      (doseq [[guid name] (map (fn [line] (cstr/split line #";"))
                               (line-seq r))]
        (jdbc/execute!
         db-repl/*db-spec*
         ["insert into rsubject
         (id, name, guid, status) values
         (gen_id(gen_rsubject, 1), ?, ?, ?);"
          name guid 1])))))
