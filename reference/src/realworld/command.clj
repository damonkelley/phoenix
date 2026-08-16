(ns realworld.command)

(defn schema [name parameters]
  [:map
   [::name [:= name]]
   [::parameters parameters]])
