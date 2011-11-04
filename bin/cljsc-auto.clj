(require 'cljs.closure)
(require 'cljsc-auto.core)

(defn transform-cl-args
  [args]
  (let [source (first args)
        opts-string (apply str (interpose " " (rest args)))
        options (when (> (count opts-string) 1)
                  (try (read-string opts-string)
                       (catch Exception e (println e))))]
    {:source source :options (merge {:output-to :print} options)}))

(let [{:keys [source options]} (transform-cl-args *command-line-args*)]
  (cljs.closure/build source options)
  (cljsc-auto.core/watch source options))
