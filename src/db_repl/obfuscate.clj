(ns db-repl.obfuscate
  (:require
   [db-repl.core :as db-repl]
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as cstr]
   [clojure.java.io :refer [resource]]))

(import com.github.javafaker.Faker)

(def faker (new Faker))

(def names
  (into (hash-map)
        (map (fn [key filename] 
               (vector key
                       (-> filename
                           resource
                           slurp
                           cstr/split-lines)))
             [:lname         :fname      :mname]
             ["surnames.txt" "names.txt" "second_names.txt"])))

(defn gen-user-data
  "Get hashmap of randomly generate user data:
  :lname, :fname, :mname, :email"
  []
  (into {:email (.. faker internet emailAddress)}
        (map (fn [k] (vector k (rand-nth (names k))))
             (keys names))))

(defn obfuscate-users []
  (doseq [user (jdbc/query db-repl/*db-spec* 
                           ["select * from ruser;"])]
    (when (not= (user :fname) "Администратор школы")
      (jdbc/update! db-repl/*db-spec* 
                    :ruser
                    (gen-user-data)
                    ["id=?" (user :id)]))))

(defn obfuscate-groups []
  (doseq [group (jdbc/query db-repl/*db-spec* ["select * from rusergroup;"])]
    (jdbc/update! db-repl/*db-spec*
                  :rusergroup
                  {:name (str (when (= (group :otype) 1)
                                (str (group :parallel) " "))
                              (.. faker company bs))}
                  ["id=?" (group :id)])))

(defn obfuscate-all []
  (db-repl/with-connection-reuse
    (obfuscate-users)
    (obfuscate-groups)))
