(ns db-repl.e5
  (:require [db-repl.core :as db-repl]
            [clojure.java.jdbc :as jdbc]))


(defn group
  "Show data about user groups"
  ([group-name]
   (db-repl/query-pprint
    ["select u.lname, g.name, utg.including_date, utg.excluding_date from rusertogroup as utg join ruser as u on u.id = utg.usrid join rusergroup as g on utg.grpid = g.id where g.name = ?;" group-name]))
  ([] (db-repl/query-pprint
       ["select u.lname, g.name, utg.including_date, utg.excluding_date from rusertogroup as utg join ruser as u on u.id = utg.usrid join rusergroup as g on utg.grpid = g.id;"])))


(defn group-history
  [& {:keys [user group]}]
  (let [show-group-history-query
        "select u.lname, g.name, utgh.include_date, 
         utgh.exclude_date
         from rusertogrouphistory as utgh
         join ruser as u on u.id = utgh.userid
         join rusergroup as g on g.id = utgh.groupid"]
    (db-repl/query-pprint
     (vec (filter identity
            (list
             (str show-group-history-query
                  (when user (format " where u.lname = ?"))
                  (when (and user group) " and")
                  (when group (format " where g.name = ?"))
                  ";")
             user
             group))))))


(defn journals
  "Get all journals belongs to given teacher"
  ([teacher-guid]
   (jdbc/query db-repl/*db-spec* 
               ["select u.guid, g.name, s.name, t.name from rjournal as j join ruser as u on u.id = j.teacher_id join rsubject as s on s.id = j.subject_id join rusergroup as g on g.id = j.group_id join rterm as t on t.id = j.term_id where u.guid = ?;" 
                teacher-guid]))
  ([] (db-repl/query-pprint
       ["select u.guid, g.name, s.name, t.name from rjournal as j join ruser as u on u.id = j.teacher_id join rsubject as s on s.id = j.subject_id join rusergroup as g on g.id = j.group_id join rterm as t on t.id = j.term_id;"])))


(defn lesson-sessions []
  (db-repl/query-pprint "select ls.id,lessonid,u.lname,intime,outtime from rlessonsess as ls join ruser as u on u.id = ls.userid;"))


(defn terms []
  (db-repl/query-pprint "select name, date_start, date_finish from rterm;"))
