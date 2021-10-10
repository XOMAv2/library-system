(ns utilities.time
  (:require ["@js-joda/timezone"] #_"It is necessary for working with timezones."
            [cljc.java-time.zone-id]
            [cljc.java-time.instant]
            [cljc.java-time.local-date-time :as local-date-time]
            [cljc.java-time.zoned-date-time :as zoned-date-time]
            [cljc.java-time.period]
            [cljc.java-time.duration]))

(defn now []
  (js/Date.))

(defn date [year month day]
  (-> (.UTC js/Date year (- month 1) day)
      (js/Date.)
      (.setUTCFullYear year)
      (js/Date.)))

(defn datetime [year month day hour minute]
  (-> (.UTC js/Date year (- month 1) day hour minute)
      (js/Date.)
      (.setUTCFullYear year)
      (js/Date.)))

(defn year-start [d]
  (-> (.UTC js/Date (.getUTCFullYear d) 0 1)
      (js/Date.)))

(defn month-start [d]
  (-> (.UTC js/Date (.getUTCFullYear d) (.getUTCMonth d) 1)
      (js/Date.)))

(defn day-start [d]
  (-> (.UTC js/Date (.getUTCFullYear d) (.getUTCMonth d) (.getUTCDate d))
      (js/Date.)))

(defn at-zone [datetime timezone]
  (-> (.toISOString datetime)
      (cljc.java-time.instant/parse)
      (local-date-time/of-instant (cljc.java-time.zone-id/of timezone))
      (local-date-time/at-zone (cljc.java-time.zone-id/of "UTC"))
      (zoned-date-time/to-instant)
      (cljc.java-time.instant/to-string)
      (js/Date.)))

(defn years
  "Obtains a time-independent Period representing a number of years."
  [n]
  (cljc.java-time.period/of-years n))

(defn months
  "Obtains a time-independent Period representing a number of months."
  [n]
  (cljc.java-time.period/of-months n))

(defn weeks
  "Obtains a time-independent Period representing a number of weeks."
  [n]
  (cljc.java-time.period/of-weeks n))

(defn days
  "Obtains a time-independent Period representing a number of days."
  [n]
  (cljc.java-time.period/of-days n))

(defn hours
  "Obtains a time-based Duration representing a number of standard hours."
  [n]
  (cljc.java-time.duration/of-hours n))

(defn minutes
  "Obtains a time-based Duration representing a number of standard minutes."
  [n]
  (cljc.java-time.duration/of-minutes n))

(defn add [date amount1 & amounts]
  (let [date (-> (.toISOString date)
                 (cljc.java-time.instant/parse)
                 (cljc.java-time.instant/at-zone (cljc.java-time.zone-id/of "UTC")))
        amounts (cons amount1 amounts)
        date (reduce #(zoned-date-time/plus % %2) date amounts)]
    (-> (zoned-date-time/to-instant date)
        (cljc.java-time.instant/to-string)
        (js/Date.))))

(defn subtract [date amount1 & amounts]
  (let [date (-> (.toISOString date)
                 (cljc.java-time.instant/parse)
                 (cljc.java-time.instant/at-zone (cljc.java-time.zone-id/of "UTC")))
        amounts (cons amount1 amounts)
        date (reduce #(zoned-date-time/minus % %2) date amounts)]
    (-> (zoned-date-time/to-instant date)
        (cljc.java-time.instant/to-string)
        (js/Date.))))
