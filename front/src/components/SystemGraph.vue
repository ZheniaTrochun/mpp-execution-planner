<template>
  <div id="graph-holder">
    <v-btn class="v-btn--outlined deep-purple add-node-btn" v-on:click="addNode">
      add node
    </v-btn>
    <v-btn class="v-btn--outlined deep-purple add-link-btn" v-on:click="addLink">
      add link
    </v-btn>
    <v-btn class="v-btn--outlined deep-purple remove-item-btn" v-on:click="deleteSelected">
      remove selected
    </v-btn>
    <v-alert class="errorDialog" type="error" v-if="isIncorrect">
      Graph is not fully connected, please make graph fully connected!
    </v-alert>
  </div>
</template>

<script>
    import cytoscape from "cytoscape";
    import store from "../store";
    import axios from "axios";

    const GRAPH_COMMIT_KEY = "persistSystemGraph";

    let graph;

    const DEFAULT_NODE_COLOR = "#666";
    const INCORRECT_NODE_COLOR = "#ff5252";

    export default {
      name: "SystemGraph",
      data() {
        return {
            isCorrect: true,
            task: {}
        };
      },
      mounted() {
        graph = cytoscape({
            container: document.getElementById("graph-holder"),
            elements: [],

            style: [
                {
                  selector: "node",
                  style: {
                    "background-color": "#666",
                    label: "data(label)"
                  }
                },
                {
                  selector: "edge",
                  style: {
                    width: 3,
                    "line-color": "#ccc",
                    "target-arrow-color": "#ccc",
                    "target-arrow-shape": "triangle",
                    'label': 'data(label)',
                    'text-margin-y': '-15px'
                  }
                },
                {
                  selector: ':selected',
                  style: {
                      'background-color': 'black',
                      'line-color': 'black',
                      'target-arrow-color': 'black',
                      'source-arrow-color': 'black',
                      'opacity': 1
                  }
                }
            ],
            maxZoom: 1,
            minZoom: 1,
            layout: {
                name: "grid",
                rows: 1
            }
        });

        this.task = store.state.selectedTask;

          if (!this.task.id) {
              this.$router.push('/');
          }

        axios.get(`https://cluster-planner-server.herokuapp.com/graphs/${this.task.id}`)
            .then(resp => {
                if (resp.status === 200) {
                    resp.data.systemGraph.forEach(x => {
                        graph.add(x);
                        this.checkGraph();
                    });

                    store.commit('initState', resp.data);
                }
            });

        this.checkGraph();

        // register deleting element on right click on it
        graph.on("cxttap", event => {
          event.preventDefault();
          const target = event.target;
          if (confirm(`Remove ${target.id()}?`)) {
            graph.remove(target);
            store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
            this.checkGraph();
          }
        });

        // register persisting element on move
        graph.on("dragfree", x => {
          console.log(x);
          store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
        });
      },
      created() {
        this.$vuetify.theme.dark = true;
      },
      computed: {
          isIncorrect() {
              return !this.isCorrect;
          }
      },
      methods: {
          addNode() {
              const name = prompt("Enter node name");
              const weight = prompt("Enter node weight");

              graph.add({
                group: "nodes",
                data: { id: name, label: `${name} - [${weight}]`, weight: weight },
                position: { x: 200, y: 200 },
              });

              store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
              this.checkGraph();
          },
          addLink() {
              const source = prompt("Source: ");
              const target = prompt("Target: ");
              const weight = prompt("Weight: ");

              graph.add({
                group: "edges",
                data: { id: `${source}-${target}`, source: source, target: target, weight: weight, label: `[${weight}]` },
              });

              store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
              this.checkGraph();
          },
          deleteSelected() {
              graph.remove(':selected');
              store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
              this.checkGraph();
          },
          deleteNode(id) {
              console.log("node clicked", id);
          },
          updateNode(event) {
              console.log("right click node", event);
          },
          preConfig(cytoscape) {
              console.log("calling pre-config", cytoscape);
          },
          afterCreated(cy) {
              // cy: this is the cytoscape instance
              console.log("after created", cy);
          },
          checkGraph() {
              if (graph) {

                  const nodes = graph.nodes().map(x => x.data());
                  const edges = graph.edges().map(x => x.data());

                  if (nodes.length === 0) {
                      return true;
                  } else {
                      const stack = new Set();
                      stack.add(nodes[0].id);

                      const traverse = (node) => {

                          if (stack.size == nodes.length) {
                              return true;
                          } else {
                              return edges
                                  .filter(e => (e.target === node) || (e.source === node))
                                  .map(e => {
                                      if (e.target === node) {
                                          return e.source;
                                      } else {
                                          return e.target;
                                      }
                                  })
                                  .filter(n => !stack.has(n))
                                  .map(n => {
                                      stack.add(n);
                                      return traverse(n);
                                  })
                                  .reduce((x, y) => x || y, false);
                          }

                      };

                      const res = traverse(nodes[0].id);

                      if (!res) {
                          graph.nodes()
                              .filter(x => !stack.has(String(x.id())))
                              .forEach(x => x.style('background-color', INCORRECT_NODE_COLOR));
                      } else {
                          graph.nodes()
                              .forEach(x => x.style('background-color', DEFAULT_NODE_COLOR));
                      }

                      this.isCorrect = res;

                      return res;
                  }
              } else {
                  return true;
              }
          }
      }
    };
</script>

<style>
    #graph-holder {
      height: 85vh;
      width: 85%;
      background-color: #fff;
    }

    .add-node-btn {
        position: absolute;
        top: 20px;
        /*right: 20px;*/
        right: -175px;
        width: 160px;
        z-index: 2;
    }

    .add-link-btn {
        position: absolute;
        top: 70px;
        /*right: 20px;*/
        right: -175px;
        width: 160px;
        z-index: 2;
    }

    .remove-item-btn {
        position: absolute;
        top: 120px;
      /*right: 20px;*/
        right: -175px;
        width: 160px;
        z-index: 2;
    }
    .errorDialog {
        position: absolute;
        width: 100%;
        z-index: 2;
    }
</style>
