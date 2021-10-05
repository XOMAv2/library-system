(ns service.frontend.icons.outline)

(defn class-concat [& classes]
  (->> classes
       (filter some?)
       (map #(if (coll? %) % [%]))
       (apply concat)))

#_"https://heroicons.com/"

(defn user-circle [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z"}]])

(defn user [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"}]])

(defn users [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"}]])

(defn book-open [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"}]])

(defn library [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z"}]])

(defn shopping-bag [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"}]])

(defn trending-up [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"}]])

(defn trash [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}]])

(defn pencil [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"}]])

(defn plus [& [{:keys [class]}]]
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class (class-concat "h-6 w-6" class), :fill "none", :viewBox "0 0 24 24", :stroke "currentColor"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M12 4v16m8-8H4"}]])
