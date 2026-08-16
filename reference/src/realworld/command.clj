(ns realworld.command)

(defn schema [name parameters]
  [:map
   [:realworld.command/name [:= name]]
   [:realworld.command/parameters parameters]])
