(ns try-to-guess.app
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rd]))

(defn app []
  [:div "App!"])

(defn ^:dev/after-load render []
  (rd/render [app]
             (.querySelector js/document "#root")))

(defn init []
  (render))
