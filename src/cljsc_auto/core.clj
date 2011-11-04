(ns cljsc-auto.core
  "Watches a directory for changes and automatically recompiles contained cljs
   files. This file depends on jpathwatch (http://sourceforge.net/projects/jpathwatch/),
   which is a backport of the file-watch functionality in JDK7."
  (:require [cljs.closure :as closure]
            [clojure.string :as str])
  (:import [java.io File]
           [java.util Date]
           [name.pachler.nio.file ClosedWatchServiceException FileSystems Paths StandardWatchEventKind]))

(defn- log [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn- path->str [path]
  (.. path (getFile) (getPath)))

(defn watch
  "Starts a thread that watches `source' and recompiles it whenever a contained
   .clj[s] file changes. The thread can be killed by stopping the watch service
   returned from this function."
  [source options]
  (let [service (.. FileSystems (getDefault) (newWatchService))
        ;; Keep mapping of registration keys to registered paths to allow
        ;; full reconstruction of affected paths.
        registrations (into {} (for [path (->> (file-seq (File. source))
                                               (filter #(.isDirectory %))
                                               (map #(Paths/get (str %))))]
                                 [(.register path service
                                             (into-array [StandardWatchEventKind/ENTRY_CREATE
                                                          StandardWatchEventKind/ENTRY_MODIFY
                                                          StandardWatchEventKind/ENTRY_DELETE]))
                                  path]))
        rebuild (fn [path]
                  (log "modified:" path)
                  (log "rebuilding...")
                  (let [start (System/nanoTime)]
                    (try
                      (when (re-find #"\.clj$" path)
                        ;; reload namespace to get latest macros
                        (let [ns-sym (-> (str/replace path
                                                      (re-pattern (format "^%s/?|\\.clj" source))
                                                      "")
                                         (str/replace #"/" ".")
                                         (str/replace #"_" "-")
                                         (symbol))]
                          (log "reloading ns" ns-sym)
                          (require :reload-all ns-sym))
                        ;; force rebuild of all sources
                        (doseq [file (reverse (file-seq (File. (closure/output-directory options))))]
                          (.delete file)))
                      (closure/build source options)
                      (log "completed in" (/ (- (System/nanoTime) start) 1e6) "ms")
                      (catch Exception e
                        (.printStackTrace e)))))
        watcher (fn []
                  (try
                    (while true
                      ;; block until event occurs
                      (let [event-key (.take service)]
                        (doseq [event (.pollEvents event-key)]
                          (if (= (.kind event) StandardWatchEventKind/OVERFLOW)
                            (log "event overflow")
                            (let [path (format "%s/%s"
                                               (path->str (registrations event-key))
                                               (path->str (.context event)))]
                              (when (re-find #"(^|/)\w+\.cljs?$" path)
                                (rebuild path)))))
                        (.reset event-key)))
                    (catch ClosedWatchServiceException e
                      (log "service closed"))))]
    (doseq [path (vals registrations)]
      (log "watching" path))
    (.start (Thread. watcher))
    service))

