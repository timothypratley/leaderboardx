(ns algopop.leaderboardx.app.io.dot
  (:require [instaparse.core :as insta]))

(def dot-gramma
  "graph : <WS> [ <'strict'> <WS> ] ('graph' | 'digraph') <WS> [ id <WS> ] <'{'> stmt_list <'}'> <WS>
<stmt_list> : <WS> [ stmt <WS> [ <';'> ] [ stmt_list ] <WS> ]
<stmt>  : node | edge | attr | eq | subgraph
eq : id <WS> <'='> <WS> id
attr : ('graph' | 'node' | 'edge') <WS> attr_list
<attr_list> : <'['> <WS> [ a_list <WS>] <']'> [ <WS> attr_list ]
<a_list>  : id <WS> <'='> <WS> id [ <WS> <(';' | ',')> ] [ <WS> a_list ]
edge : (node_id | subgraph) <WS> edgeRHS [ <WS> attr_list ]
<edgeRHS> : <edgeop> <WS> (node_id | subgraph) [ <WS> edgeRHS ]
edgeop : '->' | '--'
node : node_id [ <WS> attr_list ]
<node_id> : id [ <WS> port ]
port  : <':'> <WS> id [ <WS> <':'> <WS> compass_pt ] | <':'> <WS> compass_pt
compass_pt  : ('n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w' | 'nw' | 'c' | '_')
subgraph  : [ subgraph [ <WS> id ] <WS> ] <'{'> <WS> stmt_list <WS> <'}'>
WS : #'\\s*'
<id> : #'[a-zA-Z\\200-\\377][a-zA-Z\\200-\\377\\_0-9]*' | numeral | quoted | html
<numeral> : #'[-]?(.[0-9]+|[0-9]+(.[0-9]*)?)'
<quoted> : <'\"'> #'(?:[^\"\\\\]|\\\\.)*' <'\"'>
<html> : #'<[^>]*>'")

(def parse-dot
  (insta/parser dot-gramma))

(defn collect [graph [statement-type & statement-body]]
  (condp = statement-type
    :node (let [[id & attrs] statement-body
                attr-map (apply hash-map attrs)]
            (assoc-in graph [:nodes id] attr-map))
    :edge (let [[from to & attrs] statement-body
                attr-map (apply hash-map attrs)]
            (-> graph
                (assoc-in [:edges from to] attr-map)
                (update-in [:nodes from] merge {})
                (update-in [:nodes to] merge {})))
    :attr (merge (apply hash-map statement-body) graph)
    :eq graph
    :subgraph graph
    graph))

(defn read-graph [s]
  (let [ast (parse-dot s)]
    (if (insta/failure? ast)
      (prn "FAILURE" ast)
      (let [[_ _ & statements] ast
            [title] statements
            statements (if (string? title)
                         (rest statements)
                         statements)
            graph (if (string? title)
                    {:title title}
                    {})]
        (reduce collect graph statements)))))

(def dot
  "digraph deps {
\"dev/browser-repl\" -> \"cemerick.piggieback/cljs-repl\";
\"dev/start-figwheel\" -> \"leiningen.core.main/-main\";
\"dev/browser-repl\" -> \"weasel.repl.websocket/repl-env\";
\"dev/browser-repl\"[label=\"dev/browser-repl\"];
\"weasel.repl.websocket/repl-env\"[label=\"weasel.repl.websocket/repl-env\"];
\"dev/start-figwheel\"[label=\"dev/start-figwheel\"];
\"leiningen.core.main/-main\"[label=\"leiningen.core.main/-main\"];
\"cemerick.piggieback/cljs-repl\"[label=\"cemerick.piggieback/cljs-repl\"];}")

#_(prn "PARSE:" (parse-dot dot))
#_(prn "READ:" (read-graph dot))

(defn write-graph [g]
  (str "digraph " (:title g) " {"
       "}"))

(def dot2
  "digraph simple_hierarchy {
                            outpace_targeting_engine_propensities_impl_es->clj_http_client;
                            outpace_targeting_engine_targeting_offer_optimizer_event_based_overrides->outpace_targeting_engine_targeting_data_overrides_service;
                            outpace_targeting_engine_server->clojure_tools_logging;
                            outpace_targeting_engine_batch_histogram->clojure_tools_reader;
                            outpace_targeting_engine_targeting_offer_optimizer_driver_propensities->outpace_targeting_engine_db;
                            outpace_targeting_engine_offers_db_manage_dbs->outpace_config;
                            outpace_targeting_engine_offers_db_schema_attributes->outpace_targeting_engine_offers_db_common;
                            outpace_targeting_engine_server->outpace_auth_friend_outpace;
                            outpace_targeting_engine_offers_db_offer_find->outpace_util_core;
                            outpace_targeting_engine_offers_db_offer->java_util_Date;
                            outpace_targeting_engine_batch_upload->outpace_targeting_engine_batch_placement_group;
                            outpace_targeting_engine_offers_db_placement_group->schema_core;
                            outpace_targeting_engine_targeting_filters_locale_filter->schema_core;
                            outpace_targeting_engine_targeting_filters_active_creatives_for_placements_filter->schema_core;
                            outpace_targeting_engine_targeting_services_event_counts->outpace_targeting_engine_offers_db_schema_offers;
                            outpace_targeting_engine_targeting_flows_eval_target->outpace_targeting_engine_targeting_portfolio_optimizer_skipped_offer_annotator;
                            outpace_targeting_engine_content_offers->schema_macros;
                            outpace_targeting_engine_propensities_impl_es->clojurewerkz_elastisch_rest;
                            outpace_targeting_engine_targeting_portfolio_optimizer_offer_to_locale->schema_core;
                            outpace_targeting_engine_targeting_value_optimizer_value_propensity_aggregator->schema_core;
                            outpace_targeting_engine_seeds->outpace_db_sql;
                            outpace_targeting_engine_offers_db_schema_offers->outpace_targeting_engine_offers_db_common;
                            outpace_targeting_engine_targeting_flows_eval_target->outpace_targeting_engine_targeting_data_driver_values_service;
                            outpace_targeting_engine_seeds->outpace_targeting_engine_offers_db_schema_offers;
                            outpace_targeting_engine_offers_db_field->schema_macros;
                            outpace_offers_db_changesets_changeset_20141113180357_propensity_rules->outpace_offers_db_changesets_sugar;
                            outpace_targeting_engine_batch_util->schema_macros;
                            outpace_targeting_engine_server->outpace_auth_friend_oauth2;
                            outpace_targeting_engine_batch_processing->java_util_Date;
                            outpace_targeting_engine_batch_processing->outpace_util_core;
                            outpace_targeting_engine_targeting_data_micro_drivers_service->schema_core;
                            outpace_targeting_engine_targeting_offer_optimizer_driver_propensities->outpace_targeting_engine_propensities_schema;
                            outpace_targeting_engine_targeting_data_placement_group->outpace_util_maps;
                            outpace_targeting_engine_targeting_services_context_propensities->clojure_string;
                            outpace_targeting_engine_targeting_services_context_propensities->outpace_util_errors;
                            outpace_targeting_engine_content_server->outpace_targeting_engine_content_s3;
                            outpace_targeting_engine_batch_processing->clojure_data_csv;
                            outpace_targeting_engine_content_server->schema_core;
                            outpace_targeting_engine_targeting_flows_eval_target->outpace_targeting_engine_targeting_portfolio_optimizer_offer_to_locale;
                            outpace_targeting_engine_offers_db_flow->schema_macros;
                            outpace_targeting_engine_offers_db_offer_find->clj_time_coerce;
                            outpace_targeting_engine_targeting_data_drivers_service->outpace_util_errors;
                            outpace_targeting_engine_propensities_impl_event_counts->clojure_string;
                            outpace_targeting_engine_content_server->outpace_targeting_engine_offers_db_schema_offers;
                            outpace_targeting_engine_propensities_impl_event_counts->outpace_util_benchmark;
                            outpace_targeting_engine_offers_db_channel->outpace_targeting_engine_offers_db_common;
                            outpace_targeting_engine_targeting_flows_eval_target->outpace_targeting_engine_targeting_transformation_offer_shuffler;
                            outpace_targeting_engine_targeting_data_placement_group->schema_core;
                            outpace_targeting_engine_propensities_impl_driver_propensities->outpace_targeting_engine_propensities_schema;
                            outpace_targeting_engine_propensities_impl_driver_propensities->outpace_util_number;
                            outpace_targeting_engine_batch_targeting->schema_core;
                            outpace_targeting_engine_targeting_offer_optimizer_micro_driver_propensities->outpace_targeting_engine_targeting_flow;
                            outpace_targeting_engine_content_server->org_joda_time_LocalDate;
                            outpace_targeting_engine_batch_processing->outpace_util_logging;
                            outpace_targeting_engine_targeting_flows_eval_target->outpace_targeting_engine_targeting_test_control_force_control;
                            outpace_targeting_engine_offers_db_offer_find->outpace_util_seqs;
                            outpace_targeting_engine_targeting_portfolio_optimizer_offer_ranker->outpace_targeting_engine_targeting_flow;
                            outpace_targeting_engine_targeting_viz->loom_alg;
                            outpace_targeting_engine_targeting_flows_eval_target->outpace_targeting_engine_targeting_services_event_counts;
                            outpace_targeting_engine_seeds->outpace_db_sql_exec;
                            outpace_targeting_engine_targeting_offer_optimizer_micro_driver_propensities->outpace_util_maps;
                                                                               }")
