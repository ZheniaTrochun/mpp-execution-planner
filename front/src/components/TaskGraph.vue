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
        <v-btn class="v-btn--outlined red darken-4 generate-graph-btn" v-on:click="openGenerateDialog">
            random graph
        </v-btn>
        <p class="correlation-values">
            Sum of node weights: {{weightOfNodes}}
            <br>
            Sum of edge weights: {{weightOfEdges}}
            <br>
            correlation n: {{correlationOfWeights}}
        </p>

        <v-alert class="errorDialog" type="error" v-if="isIncorrect">
            Graph has at least one cycle, please remove all cycles!
        </v-alert>

        <v-dialog v-model="generateDialog" max-width="650">
            <v-card>
                <v-card-title>
                    <span class="headline">New graph parameters</span>
                </v-card-title>
                <v-card-text>
                    <template>
                        <v-text-field label="Number of nodes" v-model="numberOfNodes"></v-text-field>

                        <v-text-field label="Max node weight" v-model="maxNodeWeight"></v-text-field>
                        <v-text-field label="Min node weight" v-model="minNodeWeight"></v-text-field>

                        <v-text-field label="Max edge weight" v-model="maxEdgeWeight"></v-text-field>
                        <v-text-field label="Min edge weight" v-model="minEdgeWeight"></v-text-field>

                        <v-text-field label="Correlation of node and edge weights" v-model="correlation"></v-text-field>
                    </template>
                </v-card-text>
                <v-card-actions>
                    <v-spacer></v-spacer>
                    <v-btn color="v-btn--outlined deep-purple" @click="generateDialog = false">Cancel</v-btn>
                    <v-btn color="v-btn--outlined deep-purple" @click="generateTaskGraph()">Generate</v-btn>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </div>
</template>

<script>
    import cytoscape from "cytoscape";
    import store from "../store";
    import axios from 'axios';

    const GRAPH_COMMIT_KEY = 'persistTaskGraph';

    let graph;

    const DEFAULT_NODE_COLOR = "white";
    const INCORRECT_NODE_COLOR = "#ff5252";

    export default {
        name: "TaskGraph",
        data() {
            return {
                isCorrect: true,
                generateDialog: false,
                numberOfNodes: 10,
                maxNodeWeight: 10,
                minNodeWeight: 1,
                maxEdgeWeight: 5,
                minEdgeWeight: 1,
                correlation: 0.7,
                weightOfNodes: 0,
                weightOfEdges: 0,
                correlationOfWeights: 0
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
                            "background-color": "#fff",
                            'border-style': 'solid',
                            'border-color': '#666',
                            'border-width': 3,
                            label: "data(label)"
                        }
                    },
                    {
                        selector: "edge",
                        style: {
                            width: 3,
                            "line-color": "#666",
                            'curve-style': 'straight', // this needed to make it DIRECTED
                            "target-arrow-color": "#666",
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
                        resp.data.taskGraph.forEach(x => {
                            graph.add(x);
                            this.checkGraph();
                            this.calculateCorrelation();
                        });

                        store.commit('initState', resp.data);
                    }
                });

            // register deleting element on right click on it
            graph.on("cxttap", event => {
                event.preventDefault();
                const target = event.target;
                if (confirm(`Remove ${target.id()}?`)) {
                    graph.remove(target);
                    store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
                }
                this.checkGraph();
                this.calculateCorrelation();
            });

            // register persisting element on move
            graph.on("dragfree", x => {
                console.log(x);
                store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
            });

            this.checkGraph();
            this.calculateCorrelation();
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
                    position: { x: 200, y: 200 }
                });
                store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
                this.checkGraph();
                this.calculateCorrelation();
            },
            addLink() {
                const source = prompt("Source: ");
                const target = prompt("Target: ");
                const weight = prompt("Weight: ");

                graph.add({
                    group: "edges",
                    data: { id: `${source}-${target}`, source: source, target: target, weight: weight, label: `[${weight}]` }
                });
                store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
                this.checkGraph();
                this.calculateCorrelation();
            },
            deleteSelected() {
                graph.remove(':selected');
                store.commit(GRAPH_COMMIT_KEY, graph.elements().jsons());
                this.checkGraph();
                this.calculateCorrelation();
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
            markCycle(res) {
                // todo: can be a bug here
                if (res.alreadyVisited.length > 0) {
                    const duplicate = res.alreadyVisited[0];
                    let indexOfSame = 0;

                    const stackArr = Array.from(res.stack.keys());

                    for (let i = 0; i < stackArr.length; i++) {
                        if (stackArr[i] == duplicate) {
                            indexOfSame = i;
                            break;
                        }
                    }

                    const nodesToMark = new Set();

                    for (let i = indexOfSame; i < stackArr.length; i++) {
                        nodesToMark.add(stackArr[i]);
                    }

                    if (!res.result) {
                        graph.nodes()
                            .filter(x => nodesToMark.has(String(x.id())))
                            .forEach(x => x.style('background-color', INCORRECT_NODE_COLOR));
                    }
                } else {
                    graph.nodes()
                        .forEach(x => x.style('background-color', DEFAULT_NODE_COLOR));
                }
            },
            checkGraph() {
                // todo: can be a bug here
                const defaultResult = {
                    alreadyVisited: [],
                    stack: [],
                    result: true
                };

                if (graph) {
                    const nodes = graph.nodes().map(x => x.data());
                    const edges = graph.edges().map(x => x.data());

                    const traverse = (node, visited) => {

                        const nextNodes = edges
                            .filter(e => e.source === node)
                            .map(e => e.target);

                        const nodesAlreadyVisited = nextNodes.filter(x => visited.has(x));

                        if (nodesAlreadyVisited.length !== 0) {
                            return {
                                alreadyVisited: nodesAlreadyVisited,
                                stack: visited,
                                result: false
                            };
                        } else {
                            return nextNodes
                                .map(x => {
                                    const newVisited = new Set(visited);
                                    newVisited.add(x);
                                    return traverse(x, newVisited);
                                })
                                .reduce((x, y) => (!x.result) ? x : y, defaultResult);
                        }
                    };

                    const res = nodes
                        .map(x => traverse(x.id, new Set(x.id)))
                        .reduce((x, y) => (!x.result) ? x : y, defaultResult);

                    this.markCycle(res);

                    this.isCorrect = res.result;

                    return res;
                } else {
                    return defaultResult;
                }
            },
            openGenerateDialog() {
                this.generateDialog = true;
            },
            generateTaskGraph() {
                axios.put(
                    `https://cluster-planner-server.herokuapp.com/graphs/random/task-graph/${this.task.id}`,
                    {
                        minimalNodeWeight: this.minNodeWeight,
                        maximumNodeWeight: this.maxNodeWeight,
                        numberOfNodes: this.numberOfNodes,
                        correlation: this.correlation,
                        minimalEdgeWeight: this.minEdgeWeight,
                        maximumEdgeWeight: this.maxEdgeWeight
                    },
                    {
                        headers: {
                            'Content-Type': 'application/json',
                        }
                    }
                )
                    .then(resp => {
                        if (resp.status === 200) {
                            graph.remove('');

                            resp.data.taskGraph.forEach(x => {
                                graph.add(x);
                            });

                            this.checkGraph();
                            this.calculateCorrelation();
                            store.commit('initState', resp.data);
                            this.generateDialog = false;
                        }
                    });
            },
            calculateCorrelation() {
                if (graph) {
                    this.weightOfNodes = graph.nodes().map(x => x.data()).map(node => parseInt(node.weight)).reduce((x, y) => x + y, 0);
                    this.weightOfEdges = graph.edges().map(x => x.data()).map(edge => parseInt(edge.weight)).reduce((x, y) => x + y, 0);

                    const sum = this.weightOfNodes + this.weightOfEdges;

                    this.correlationOfWeights = sum === 0 ? 0 : (this.weightOfNodes / sum);
                }
            }
        }
    };
</script>

<style>
    #graph-holder {
        height: 85vh;
        width: 85%;
        margin-right: 180px;
        background-color: #fff;
    }

    .add-node-btn {
        position: absolute;
        top: 20px;
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
        z-index: 200;
    }

    .generate-graph-btn {
        position: absolute;
        top: 170px;
        /*right: 20px;*/
        right: -175px;
        width: 160px;
        z-index: 200;
    }

    .errorDialog {
        position: absolute;
        width: 100%;
        z-index: 2;
    }

    .correlation-values {
        position: absolute;
        top: 30px;
        right: 100px;
        width: 200px;
        z-index: 2;
        color: black;
        font-size: large;
    }
</style>
