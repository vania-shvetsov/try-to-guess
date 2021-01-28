(ns try-to-guess.state
  (:require [re-frame.core :as rf]
            [clojure.string :as string]))

(def hints-count 5)
(def seconds-per-hint 5)

(def words
  [{:word "концентрация"
    :hints ["фокусировка" "фиксация" "один" "направление" "насыщенный"]}
   {:word "культура"
    :hints ["просвященность" "цивилизация" "театр" "литература" "канал"]}
   {:word "холм"
    :hints ["возвышенность" "выпуклость" "вершина" "пригорок" "бугор"]}
   {:word "галерея"
    :hints ["музей" "коридор" "проход" "сооружение" "хранилище"]}
   {:word "масленица"
    :hints ["праздник" "плодородие" "весна" "сани" "блины"]}])

(def initial-db
  {:words words
   :round {:word nil
           :using-hints []
           :timer 0
           :status :initial ;; :guessing :missing :correct
           :failed-attempts []}
   :initial-timer (* hints-count seconds-per-hint)
   :next-hint-timeout seconds-per-hint})

(rf/reg-event-db
 ::init
 (fn [_ _] initial-db))

(defn- start-round [db word-index]
  (let [{:keys [initial-timer]} db
        guessed-word word-index]
    {:db
     (assoc db :round {:word guessed-word
                       :timer initial-timer
                       :status :guessing
                       :failed-attempts []
                       :using-hints []})
     :dispatch [::_tick-timer]}))

(rf/reg-event-fx
 ::start-new-round
 (fn [{db :db} _]
   (let [{round :round} db]
     (start-round db
                  (if-let [w (:word round)] (inc w) 0)))))

(rf/reg-event-fx
 ::restart-round
 (fn [{db :db} _]
   (start-round db (get-in db [:round :word]))))

(rf/reg-event-fx
 ::_tick-timer
 (fn [{db :db} _]
   (let [{:keys [round]} db
         {:keys [timer status]} round
         next-time (dec timer)]
     (if (= status :guessing)
       (if (pos? next-time)
         (let [{next-hint-timeout :next-hint-timeout} db
               using-hints (:using-hints round)
               using-hints' (if (zero? (mod timer next-hint-timeout))
                              (conj using-hints (count using-hints))
                              using-hints)]
           {:db (-> db
                    (assoc-in [:round :timer] next-time)
                    (assoc-in [:round :using-hints] using-hints'))
            :dispatch-later {:ms 1000 :dispatch [::_tick-timer]}})
         {:db (-> db
                  (assoc-in [:round :timer] next-time)
                  (assoc-in [:round :status] :missing))})
       {:db db}))))

(rf/reg-event-db
 ::make-attempt
 (fn [db [_ supposed-word]]
   (let [supposed-word' (string/lower-case supposed-word)
         {:keys [round words]} db
         guessed-word (get-in words [(:word round) :word])]
     (if (= supposed-word' guessed-word)
       (assoc-in db [:round :status] :correct)
       (update-in db [:round :failed-attemps] conj supposed-word')))))

(rf/reg-sub
 ::hints
 (fn [db]
   (let [word-index (get-in db [:round :word])
         using-hints-indexes (get-in db [:round :using-hints])
         word-config (get-in db [:words word-index])
         {:keys [hints]} word-config
         using-hints (map #(get hints %) using-hints-indexes)]
     using-hints)))

(rf/reg-sub
 ::timer
 (fn [db]
   (get-in db [:round :timer])))

(rf/reg-sub
 ::game-status
 (fn [db]
   (get-in db [:round :status])))

(rf/reg-sub
 ::has-words?
 (fn [db]
   (let [{:keys [round words]} db]
     (< (:word round) (dec (count words))))))

(rf/reg-sub
 ::hint-timeout
 (fn [db]
   (:next-hint-timeout db)))

(comment
  (rf/dispatch [::init])
  (rf/dispatch [::start-new-round])
  (rf/dispatch [::make-attempt]))
