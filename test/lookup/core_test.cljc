(ns lookup.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [lookup.core :as sut]))

(deftest parse-selector-test
  (is (= (sut/parse-selector 'div) {:tag-name "div"}))
  (is (= (sut/parse-selector :my/alias) {:tag-name "my/alias"}))
  (is (= (sut/parse-selector :div) {:tag-name "div"}))

  (is (= (sut/parse-selector :div#app)
         {:tag-name "div" :id "app"}))

  (is (= (sut/parse-selector :div.btn)
         {:tag-name "div" :class #{"btn"}}))

  (is (= (sut/parse-selector 'div.btn.btn-primary)
         {:tag-name "div" :class #{"btn" "btn-primary"}}))

  (is (= (sut/parse-selector '.btn) {:class #{"btn"}}))

  (is (= (sut/parse-selector 'div#app.btn.btn-primary)
         {:tag-name "div" :id "app" :class #{"btn" "btn-primary"}}))

  (is (= (sut/parse-selector 'div.btn#app.btn-primary)
         {:tag-name "div" :id "app" :class #{"btn" "btn-primary"}}))

  (is (= (sut/parse-selector "div[id]")
         {:tag-name "div" :attrs #{{:attr :id}}}))

  (is (= (sut/parse-selector "div[id].btn[alt]")
         {:tag-name "div"
          :attrs #{{:attr :alt} {:attr :id}}
          :class #{"btn"}}))

  (is (= (sut/parse-selector "div[id=app]")
         {:tag-name "div"
          :attrs #{{:attr :id :f "=" :val "app"}}}))

  (is (= (sut/parse-selector ":first-child")
         {:pseudo-class #{"first-child"}}))

  (is (= (sut/parse-selector ".btn:first-child:last-child")
         {:class #{"btn"}
          :pseudo-class #{"first-child" "last-child"}})))

(deftest get-hiccup-headers-test
  (is (= (sut/get-hiccup-headers [:div]) {:tag-name "div"}))

  (is (= (sut/get-hiccup-headers [:div.btn])
         {:tag-name "div" :class #{"btn"}}))

  (is (= (sut/get-hiccup-headers [:div#id.btn])
         {:tag-name "div" :id "id" :class #{"btn"}}))

  (is (= (sut/get-hiccup-headers [:div#id.btn {:class "ok"}])
         {:tag-name "div" :id "id" :class #{"ok" "btn"}}))

  (is (= (sut/get-hiccup-headers [:div#id.btn {:class ["ok"]}])
         {:tag-name "div" :id "id" :class #{"ok" "btn"}}))

  (is (= (sut/get-hiccup-headers [:div {:class ["ok"]}])
         {:tag-name "div" :class #{"ok"}}))

  (is (= (sut/get-hiccup-headers [:div {:class "btn  btn-primary"}])
         {:tag-name "div" :class #{"btn" "btn-primary"}})))

(defn matches? [selector hiccup]
  (sut/matches? (sut/parse-selector selector) hiccup))

(deftest matches?-test
  (is (true? (matches? 'div [:div])))
  (is (false? (matches? 'div [:p])))
  (is (true? (matches? 'button.btn [:button {:class "btn"}])))
  (is (true? (matches? 'button.btn [:button.btn])))
  (is (false? (matches? 'button.button [:button.btn])))
  (is (true? (matches? 'button.btn [:button.btn.btn-primary])))
  (is (true? (matches? 'button.btn.btn-primary [:button.btn.btn-primary])))
  (is (false? (matches? 'button.btn.btn-primary [:button.btn])))
  (is (true? (matches? "#app" [:div {:id "app"}])))
  (is (true? (matches? "#app" [:div#app])))
  (is (false? (matches? "#application" [:div#app])))
  (is (true? (matches? "h1[title]" [:h1 {:title "Hello"} "Hello"])))
  (is (false? (matches? "h1[title]" [:h1 "Hello"])))
  (is (true? (matches? "h1[data-text]" [:h1 {:data-text "Hello"} "Hello"])))
  (is (false? (matches? "h1[data-text]" [:h1 {:data-title "Hey"} "Hello"])))
  (is (true? (matches? "h1[data-text=Hello]" [:h1 {:data-text "Hello"} "Hello"])))
  (is (false? (matches? "h1[data-text=hello]" [:h1 {:data-text "Hello"} "Hello"])))
  (is (true? (matches? "h1[class=title]" [:h1.title "Hello"])))
  (is (false? (matches? "h1[class=title]" [:h1.title.heading "Hello"])))
  (is (true? (matches? "h1[data-text~=Hello]" [:h1 {:data-text "Hello"} "Hello"])))
  (is (true? (matches? "h1[data-text~=abc]" [:h1 {:data-text "abc def ghi"} "Hello"])))
  (is (true? (matches? "h1[data-text~=def]" [:h1 {:data-text "abc def ghi"} "Hello"])))
  (is (true? (matches? "h1[data-text~=ghi]" [:h1 {:data-text "abc def ghi"} "Hello"])))
  (is (false? (matches? "h1[data-text~=def ghi]" [:h1 {:data-text "abc def ghi"} "Hello"])))
  (is (true? (matches? "h1[class~=title]" [:h1.title "Hello"])))
  (is (true? (matches? "h1[lang|=nb-NO]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (true? (matches? "h1[lang|=nb]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang|=nb-]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang|=en]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (true? (matches? "h1[class|=btn]" [:h1.btn-primary "Hello"])))
  (is (false? (matches? "h1[class|=btn]" [:h1.lol-btn-primary "Hello"])))
  (is (true? (matches? "h1[lang^=nb]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang^=en]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang^=btn]" [:h1.btn "Hello"])))
  (is (true? (matches? "h1[class^=btn]" [:h1.btn-primary "Hello"])))
  (is (true? (matches? "h1[lang$=NO]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang$=GB]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang$=btn]" [:h1.btn "Hello"])))
  (is (true? (matches? "h1[class$=primary]" [:h1.btn-primary "Hello"])))
  (is (true? (matches? "h1[lang*=NO]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang*=GB]" [:h1 {:lang "nb-NO"} "Hello"])))
  (is (false? (matches? "h1[lang*=btn]" [:h1.btn "Hello"])))
  (is (true? (matches? "h1[class*=primary]" [:h1.btn-primary-lul "Hello"]))))