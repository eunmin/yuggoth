(ns yuggoth.config
  (:use clojure.java.io yuggoth.models.schema yuggoth.locales environ.core)
  (:import java.io.File
           java.sql.DriverManager
           org.postgresql.ds.PGPoolingDataSource))

(def blog-config (atom nil))
(def db (atom nil))

(defn text [tag]
  (get (get dict (get @blog-config :locale :en)) tag "<no translation available>"))

(defn load-config-file []
  (let [url (resource "blog.properties")]
    (if (or (nil? url) (.. url getPath (endsWith "jar!/blog.properties")))
      (doto (new File "blog.properties") (.createNewFile))
      url)))

(defn reset [config]
  (reset! db
          {:datasource
           (doto (new PGPoolingDataSource)
             (.setServerName   (:host config) )
             (.setDatabaseName (:schema config))
             (.setPortNumber   (:port config ))
             (.setUser         (:user config))
             (.setPassword     (:pass config)))})
  (reset! blog-config (select-keys config [:ssl :ssl-port :initialized :locale])))

(defn init []
  (if-let [host (env :yug-host)]
    (reset (hash-map :host host,
                     :schema (env :yug-schema)
                     :port (Integer/valueOf (env :yug-port))
                     :user (env :yug-user)
                     :pass (env :yug-password)
                     :ssl (Boolean/valueOf (env :yug-ssl))
                     :ssl-port (Integer/valueOf (env :yug-ssl-port))
                     :initialized (Boolean/valueOf (env :yug-initialized))
                     :locale (keyword (env :yug-locale))
                     ))
    (with-open
        [r (java.io.PushbackReader. (reader (load-config-file)))]
      (if-let [config (read r nil nil)]
        (reset config)))
    )

  (println "intialized"))

(defn save [config]
  (with-open [con (DriverManager/getConnection
                    (str "jdbc:postgresql://" (:host config) "/" (:schema config)) (:user config) (:pass config))])
  (with-open [w (clojure.java.io/writer (load-config-file))]
    (.write w (str config))
    (reset config)))
