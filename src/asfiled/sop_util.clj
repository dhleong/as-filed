(ns ^{:author "Daniel Leong"
      :doc "Utilities for SOPs"}
  asfiled.sop-util
  (:require [clojure.string :refer [join]]))

(defn- within-range
  [candidate range-array]
  (<= (first range-array) candidate (last range-array)))

(defn match-runway
  [weather runway-descriptor]
  (let [runway (:when runway-descriptor)
        true-dir (:dir weather)]
    (and
      ;; speed 
      (within-range (:speed weather) (:speed runway))
      ;; direction
      (or
        (= :variable (:dir weather))
        (seq 
          (filter 
            #(within-range (:dir weather) %)
            (:dir runway))))
      ;; rvr
      (or
        ;; eg: both nil
        (= (:rvr weather) (:rvr runway))
        ;; rvr range
        (and
          (not (nil? (:rvr runway)))
          (not (nil? (:rvr weather)))
          (within-range (:rvr weather) (:rvr runway)))))))

(defn- has-tag
  [vect tag]
  (some #(= % tag) vect))

(defn format-sids
  [sop sid]
  (if-let [numbers (:departure-numbers sop)]
    (reduce 
      (fn [result [proc number]]
        (-> result (.replace (str proc "#") (str proc number))))
      sid
      numbers)
    ;; nope; just be normal
    sid))

(defn match-tag
  [tag tag-descriptor]
  (if (map? tag-descriptor)
    ;; :any or :not
    (case (-> tag-descriptor keys first)
      :any (has-tag (:any tag-descriptor) tag)
      :not (not (has-tag (:not tag-descriptor) tag)))
    ;; exact match
    (= tag tag-descriptor)))

(defn match-sid
  [tags sid-descriptor]
  (let [tag-descriptor (:when sid-descriptor)]
    (when (= (count tags) (count tag-descriptor))
      (every? true? (map #(match-tag %1 %2) tags tag-descriptor)))))

(defn get-common-amendments
  "Given a route and an SOP with common amendments,
  return a list of [point amendment] pairs of common
  amendments that may be applicable for that route.
  Returns an empty sequence if there were no matches"
  [sop route]
  (when-let [amends (:common-ammendments sop)]
    (let [parts (re-seq #"\w+" route)
          part1 (first parts)
          part2 (second parts)
          part3 (nth parts 2 nil)]
      (->> [[part1 (get amends part1)]
            [part2 (get amends part2)]
            [part3 (get amends part3)]]
           (filter 
             (fn [[_ amend]] 
               (and
                 ;; filter out non-existing entries...
                 amend
                 ;; and unnecessary ones
                 (= -1 (-> route (.indexOf amend))))))))))

(defn select-runways
  "Given weather conditions and an SOP,
  figure out what runway configuration
  should be in use. 
  Weather should look like:
  {:speed 3 :dir 240 :rvr 3500}
  :rvr may be omitted if none specified. Direction should
  be relative to true north if :magnetic-variation is 
  specified in the SOP; otherwise, it should be relative
  to magnetic north
  Returns:
  {:runways 'string description', :tags [:depart-1 :land-2]}"
  [sop weather]
  (when-let [runways (:runway-selection sop)]
    (let [true-dir (:dir weather)
          mag-var (:magnetic-variation sop 0)
          adjusted-weather
          (if (keyword? true-dir)
            weather ;; nothing to do
            (assoc weather
                   :dir (+ true-dir mag-var)))]
      (when-let [matched (filter 
                           #(match-runway adjusted-weather %)
                           runways)]
       {:runways (join "\n" (map :use matched))
        :tags (apply concat (map :tags matched))
        :w adjusted-weather}))))

(defn select-sid
  "Given a set of tags (as returned from select-runways)
  and an SOP, figure out which SID should be used."
  [sop tags]
  (when-let [sids (:sid-selection sop)]
    (when-let [matched (filter #(match-sid tags %) sids)]
      (join "\n\n" 
            (map 
              #(->> % :use (format-sids sop))
              matched)))))
