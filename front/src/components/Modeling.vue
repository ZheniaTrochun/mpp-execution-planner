<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div class="main-wrapper">
        <div id="immutable-task-graph-holder">
            <v-alert class="errorDialog" type="error" v-if="isIncorrect">
                Graph has at least one cycle, please remove all cycles!
            </v-alert>
        </div>

        <v-tabs
                v-model="tab"
                background-color="deep-purple accent-4"
                class="elevation-2 full-width"
                dark
                :centered="centered"
                :grow="grow"
                :vertical="vertical"
                :right="right"
                :prev-icon="prevIcon ? 'mdi-arrow-left-bold-box-outline' : undefined"
                :next-icon="nextIcon ? 'mdi-arrow-right-bold-box-outline' : undefined"
                :icons-and-text="icons"
        >
            <v-tabs-slider></v-tabs-slider>

            <v-tab v-on:click="modelQueueBasedOnCriticalPath()">
                Algorithm 3
            </v-tab>

            <v-tab-item>
                <v-card flat tile>
                    <v-simple-table>
                        <template v-slot:default>
                            <thead>
                            <tr>
                                <th class="text-left">Node</th>
                                <th class="text-left">Critical path to end of task</th>
                                <th class="text-left">Critical path length</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="item in criticalPathQueue" :key="item.node.id">
                                <td>{{ item.node.id }}</td>
                                <td>{{ item.path }}</td>
                                <td>{{ item.value }}</td>
                            </tr>
                            </tbody>
                        </template>
                    </v-simple-table>
                </v-card>
            </v-tab-item>

            <v-tab v-on:click="modelQueueBasedOnCriticalPathByNodesCount()">
                Algorithm 5
            </v-tab>

            <v-tab-item>
                <v-card flat tile>
                    <v-simple-table>
                        <template v-slot:default>
                            <thead>
                            <tr>
                                <th class="text-left">Node</th>
                                <th class="text-left">Critical path by number of nodes</th>
                                <th class="text-left">Critical path length</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="item in criticalPathByNumberOfNodesQueue" :key="item.node.id">
                                <td>{{ item.node.id }}</td>
                                <td>{{ item.path }}</td>
                                <td>{{ item.value }}</td>
                            </tr>
                            </tbody>
                        </template>
                    </v-simple-table>
                </v-card>
            </v-tab-item>

            <v-tab v-on:click="modelQueueBasedOnConnectivity()">
                Algorithm 10
            </v-tab>

            <v-tab-item>
                <v-card flat tile>
                    <v-simple-table>
                        <template v-slot:default>
                            <thead>
                            <tr>
                                <th class="text-left">Node</th>
                                <th class="text-left">Node connectivity</th>
                                <th class="text-left">Critical path length</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="item in queueByConnectivity" :key="item.node.id">
                                <td>{{ item.node.id }}</td>
                                <td>{{ item.connectivity }}</td>
                                <td>{{ item.pathLength }}</td>
                            </tr>
                            </tbody>
                        </template>
                    </v-simple-table>
                </v-card>
            </v-tab-item>

        </v-tabs>
    </div>
</template>

<script>
    import cytoscape from "cytoscape";
    import store from "../store";
    import axios from 'axios';

    let graph;

    const DEFAULT_NODE_COLOR = "white";
    const INCORRECT_NODE_COLOR = "#ff5252";

    export default {
        name: "Modeling",
        data () {
            return {
                tab: null,
                text: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.',
                icons: false,
                centered: false,
                grow: true,
                vertical: false,
                prevIcon: true,
                nextIcon: true,
                right: false,
                tabs: 3,
                isCorrect: true,

                criticalPathQueue: [],
                criticalPathByNumberOfNodesQueue: [],
                queueByConnectivity: []
            }
        },
        mounted() {
            graph = cytoscape({
                container: document.getElementById("immutable-task-graph-holder"),

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
                        });

                        graph.autolock(true);
                        store.commit('initState', resp.data);
                    }
                });

            this.checkGraph();
            this.modelQueueBasedOnCriticalPath();
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
            modelQueueBasedOnCriticalPath() {

                axios.get(`https://cluster-planner-server.herokuapp.com/queue/critical-path/${this.task.id}`)
                    .then(resp => {
                        if (resp.status === 200) {
                            console.log(resp.data);

                            this.criticalPathQueue = resp.data.map(item => {
                                return {
                                    node: item[0],
                                    path: item[1].map(node => node.id).reduce((acc, curr) => acc + ' -> ' + curr),
                                    value: item[1].map(node => Number(node.weight)).reduce((acc, curr) => acc + curr)
                                }
                            });
                        }
                    });
            },
            modelQueueBasedOnCriticalPathByNodesCount() {

                axios.get(`https://cluster-planner-server.herokuapp.com/queue/node-count-on-critical-path/${this.task.id}`)
                    .then(resp => {
                        if (resp.status === 200) {
                            console.log(resp.data);

                            this.criticalPathByNumberOfNodesQueue = resp.data.map(item => {
                                return {
                                    node: item[0],
                                    path: item[1].map(node => node.id).reduce((acc, curr) => acc + ' -> ' + curr),
                                    value: item[1].length
                                }
                            });
                        }
                    });
            },
            modelQueueBasedOnConnectivity() {

                axios.get(`https://cluster-planner-server.herokuapp.com/queue/node-connectivity/${this.task.id}`)
                    .then(resp => {
                        if (resp.status === 200) {
                            console.log(resp.data);

                            this.queueByConnectivity = resp.data.map(item => {
                                return {
                                    node: item[0],
                                    connectivity: item[1][0],
                                    pathLength: item[1][1]
                                }
                            });
                        }
                    });
            },
            markCycle(res) {
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
            }
        }
    };
</script>

<style scoped>

    #immutable-task-graph-holder {
        width: 100%;
        background-color: white;
        height: 60vh;
    }

    .full-width {
        width: 100%;
    }

    .main-wrapper {
        width: 100%;
    }
</style>