(defproject db-repl "0.1.0-SNAPSHOT"
  :description "Useful tool to explore and edit databases"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.firebirdsql.jdbc/jaybird-jdk17 "2.2.6"]
                 [com.github.javafaker/javafaker "0.17.2"]]
  :repl-options {:init-ns db-repl.main})
