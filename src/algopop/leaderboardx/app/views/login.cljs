(ns algopop.leaderboardx.app.views.login
  (:require
    [algopop.leaderboardx.app.firebase :as firebase]))

(defn login-view []
  [:div
   {:style {:float "right"}}
   (if-let [{:keys [photo-url display-name]} @firebase/user]
     [:span
      [:button.mdl-button.mdl-js-button.mdl-button--fab.mdl-button--colored
       {:on-click
        (fn logout-click [e]
          (firebase/sign-out))
        :title display-name
        :style {:width "30px"
                :height "30px"
                :background-image (str "url(" photo-url ")")
                :background-size "cover"
                :background-repeat "no-repeat"}}]]
     [:button.mdl-button.mdl-button--raised.mdl-button--colored
      {:on-click
       (fn login-click [e]
         (firebase/sign-in))}
      "Login with Google"])])
