(ns try-to-guess.app
  (:require [re-frame.core :as rf]
            [clojure.string :as string]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [try-to-guess.state]))

(defn start-new-game []
  [:div
   [:button.bg-gray-500.text-white.px-3.h-8.rounded-md
    {:on-click #(rf/dispatch [:try-to-guess.state/start-new-round])}
    "Начать"]])

(defn input-attempt-control []
  (let [value (r/atom "")
        handle-change (fn [e]
                        (reset! value (-> e .-target .-value)))]
    (fn []
      [:div
       [:input.h-8.rounded-md.bg-gray-100.px-3.w-72
        {:on-change handle-change
         :placeholder "Какое слово загадано?"}]
       [:button.bg-gray-500.text-white.px-3.h-8.rounded-md.ml-2
        {:on-click (fn []
                     (let [value' (string/trim @value)]
                       (when (pos? (count value'))
                         (rf/dispatch [:try-to-guess.state/make-attempt value']))))}
        "Проверить"]])))

(defn hints []
  (r/with-let [hint-words (rf/subscribe [:try-to-guess.state/hints])]
    (when (pos? (count @hint-words))
      [:div.flex.flew-wrap.mt-4
       (map-indexed (fn [i el]
                      [:div.p-4.bg-gray-500.text-white.flex-shrink.rounded-md.mr-2
                       {:key i}
                       el])
                    @hint-words)])))

(defn guessing-process []
  [:div
   [input-attempt-control]
   [hints]])

(defn missing-word []
  [:div
   [:div.mb-4
    "Время вышло :("]
   [:div
    [:button.bg-gray-500.text-white.px-3.h-8.rounded-md
     {:on-click #(rf/dispatch [:try-to-guess.state/restart-round])}
     "Попробывать еще раз"]]])

(defn correct-guessing []
  (r/with-let [has-words? (rf/subscribe [:try-to-guess.state/has-words?])]
    [:div
     [:div.mb-4
      "Ура! Правильно!"]
     (if @has-words?
       [:button.bg-gray-500.text-white.px-3.h-8.rounded-md
        {:on-click #(rf/dispatch [:try-to-guess.state/start-new-round])}
        "Следующее слово"]
       [:div "Все! Больше слов нет!"])]))

(defn description-text []
  (r/with-let [hint-timeout (rf/subscribe [:try-to-guess.state/hint-timeout])]
    [:div
     [:div "Попробуй отгадать слово, используя слова-подсказки"]
     [:div.mb-8.text-sm.text-gray-400
      "Каждые "
      @hint-timeout
      " секунд будет появляться новая подсказка"]]))

(defn app []
  (r/with-let [status (rf/subscribe [:try-to-guess.state/game-status])]
    [:div.container.mx-auto.py-5.max-w-screen-sm
     [description-text]
     (case @status
       :initial [start-new-game]
       :guessing [guessing-process]
       :missing [missing-word]
       :correct [correct-guessing])]))

(defn ^:dev/after-load render []
  (rd/render [app]
             (.querySelector js/document "#root")))

(defn init []
  (rf/dispatch-sync [:try-to-guess.state/init])
  (render))

(comment
  (init))
