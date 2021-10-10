(ns service.frontend.ui.styles)

(def input-style
  "block w-full py-2 px-3 border-2 border-blue-500 text-sm
   font-medium rounded-md focus:ring-2 focus:ring-offset-2
   focus:ring-blue-500 focus:outline-none bg-blue-50
   disabled:bg-gray-100 disabled:text-gray-500")

(def icon-button-style
  "rounded-xl transform hover:scale-125 focus:scale-125 focus:outline-none
   disabled:text-gray-500"
  #_"TODO: tailwind combine prefixes"
  #_"hover:disabled:scale-100")

(def button-style
  "py-2 px-4 text-sm font-medium rounded-md text-white
   bg-blue-500 hover:bg-blue-600 focus:outline-none
   focus:ring-2 focus:ring-offset-2 focus:ring-blue-500
   disabled:bg-blue-400")

(def outline-button-style
  "py-2 px-3 text-sm rounded-md
   font-medium focus:ring-2 focus:ring-offset-2
   focus:ring-blue-500 focus:outline-none
   bg-blue-50 hover:bg-blue-200
   disabled:bg-gray-100 disabled:text-gray-500")

(def card-style
  "shadow-md rounded-xl p-6 bg-white w-[26rem]")

(def link-style
  "hover:underline text-blue-500")

(def entity-item-style
  "py-2 px-3 bg-blue-50 rounded-xl space-y-1
   hover:bg-blue-200 focus:ring-2 focus:ring-offset-2
   focus:ring-blue-500 focus:outline-none")

(def chip-style
  "rounded-full px-3 border text-md")

(def chip-colors
  [["bg-gray-100 border-gray-500 hover:bg-gray-200"]
   ["bg-red-100  border-red-500 hover:bg-red-200"]
   ["bg-yellow-100 border-yellow-500 hover:bg-yellow-200"]
   ["bg-green-100 border-green-500 hover:bg-green-200"]
   ["bg-blue-100 border-blue-500 hover:bg-blue-200"]
   ["bg-indigo-100 border-indigo-500 hover:bg-indigo-200"]
   ["bg-purple-100 border-purple-500 hover:bg-purple-200"]
   ["bg-pink-100 border-pink-500 hover:bg-pink-200"]])