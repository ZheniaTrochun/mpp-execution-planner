<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div class="main-wrapper">
        <div id="immutable-task-graph-holder">
            <v-alert class="errorDialog" type="error" v-if="isTaskIncorrect">
                Graph has at least one cycle, please remove all cycles!
            </v-alert>
        </div>

        <div id="immutable-system-graph-holder">
            <v-alert class="errorDialog" type="error" v-if="isSystemIncorrect">
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

            <v-tab v-on:click="planningBasedOnConnectivity()">
                Planning based on node connectivity
            </v-tab>

            <v-tab-item>
                <v-card flat tile>
                    <canvas id="ghant-diagram-connectivity"></canvas>
                </v-card>
            </v-tab-item>

<!--            <v-tab v-on:click="modelQueueBasedOnCriticalPathByNodesCount()">-->
<!--                Algorithm 5-->
<!--            </v-tab>-->

<!--            <v-tab-item>-->
<!--                <v-card flat tile>-->
<!--                    <v-simple-table>-->
<!--                        <template v-slot:default>-->
<!--                            <thead>-->
<!--                            <tr>-->
<!--                                <th class="text-left">Node</th>-->
<!--                                <th class="text-left">Critical path by number of nodes</th>-->
<!--                                <th class="text-left">Critical path length</th>-->
<!--                            </tr>-->
<!--                            </thead>-->
<!--                            <tbody>-->
<!--                            <tr v-for="item in criticalPathByNumberOfNodesQueue" :key="item.node.id">-->
<!--                                <td>{{ item.node.id }}</td>-->
<!--                                <td>{{ item.path }}</td>-->
<!--                                <td>{{ item.value }}</td>-->
<!--                            </tr>-->
<!--                            </tbody>-->
<!--                        </template>-->
<!--                    </v-simple-table>-->
<!--                </v-card>-->
<!--            </v-tab-item>-->

        </v-tabs>
    </div>
</template>

<script>
    import cytoscape from "cytoscape";
    import store from "../store";
    import axios from 'axios';

    let taskGraph;
    let systemGraph;

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
                isTaskCorrect: true,
                isSystemCorrect: true,

                criticalPathQueue: [],
                criticalPathByNumberOfNodesQueue: [],
                queueByConnectivity: [],

                ghantDiagram: [],

                selectedQueueCreationAlgo: ""
            }
        },
        mounted() {
            taskGraph = cytoscape({
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

            systemGraph = cytoscape({
                container: document.getElementById("immutable-system-graph-holder"),
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
                        resp.data.taskGraph.forEach(x => {
                            taskGraph.add(x);
                        });

                        resp.data.systemGraph.forEach(x => {
                            systemGraph.add(x);
                        });

                        taskGraph.autolock(true);
                        systemGraph.autolock(true);

                        store.commit('initState', resp.data);
                    }
                });

            this.checkGraphs();
            this.modelQueueBasedOnCriticalPath();
            this.planningBasedOnConnectivity();
        },
        created() {
            this.$vuetify.theme.dark = true;
        },
        computed: {
            isTaskIncorrect() {
                return !this.isTaskCorrect;
            },
            isSystemIncorrect() {
                return !this.isSystemCorrect;
            }
        },
        methods: {
            modelQueueBasedOnCriticalPath() {

                this.selectedQueueCreationAlgo = "critical-path";

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

                this.selectedQueueCreationAlgo = "node-count-on-critical-path";

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

                this.selectedQueueCreationAlgo = "node-connectivity";

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
                        taskGraph.nodes()
                            .filter(x => nodesToMark.has(String(x.id())))
                            .forEach(x => x.style('background-color', INCORRECT_NODE_COLOR));
                    }
                } else {
                    taskGraph.nodes()
                        .forEach(x => x.style('background-color', DEFAULT_NODE_COLOR));
                }
            },
            checkGraphs() {
                this.checkTaskGraph();
                this.checkSystemGraph();
            },
            checkSystemGraph() {
                if (systemGraph) {

                    const nodes = systemGraph.nodes().map(x => x.data());
                    const edges = systemGraph.edges().map(x => x.data());

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
                            systemGraph.nodes()
                                .filter(x => !stack.has(String(x.id())))
                                .forEach(x => x.style('background-color', INCORRECT_NODE_COLOR));
                        } else {
                            systemGraph.nodes()
                                .forEach(x => x.style('background-color', DEFAULT_NODE_COLOR));
                        }

                        this.isSystemCorrect = res;

                        return res;
                    }
                } else {
                    return true;
                }
            },
            checkTaskGraph() {
                const defaultResult = {
                    alreadyVisited: [],
                    stack: [],
                    result: true
                };

                if (taskGraph) {
                    const nodes = taskGraph.nodes().map(x => x.data());
                    const edges = taskGraph.edges().map(x => x.data());

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

                    this.isTaskCorrect = res.result;

                    return res;
                } else {
                    return defaultResult;
                }
            },
            planningBasedOnConnectivity() {
                axios.get(`https://cluster-planner-server.herokuapp.com/planning/${this.selectedQueueCreationAlgo}/connectivity/${this.task.id}`)
                    .then(resp => {
                        if (resp.status === 200) {
                            console.log(resp.data);

                            this.ghantDiagram = resp.data.entries;

                            this.drawGhantDiagram(resp.data.entries, "ghant-diagram-connectivity");
                        }
                    });
            },
            drawGhantDiagram(elements, canvasId) {
                const canvas = document.getElementById(canvasId);
                const ctx = canvas.getContext('2d');
                ctx.clearRect(0,0,canvas.width,canvas.height);
                ctx.font="20px Arial";

                canvas.height = screen.height * 0.6;// 1000;
                // canvas.width = 1000;
                // canvas.width = screen.width - 100;
                canvas.width = document.getElementById("immutable-task-graph-holder").offsetWidth;

                // ctx.scale(1, 1);
                const numberOfProcessors = systemGraph.nodes().size();

                console.log(numberOfProcessors);

                ctx.beginPath();

                for (let i = 0; i < numberOfProcessors; i++) {
                    ctx.fillText(`${i + 1}`, 50, i * 50 + 75);
                    ctx.moveTo(85, i * 50 + 75);
                    ctx.lineTo(canvas.width - 100, i * 50 + 75);
                }

                ctx.moveTo(85, 50);
                ctx.lineTo(85, numberOfProcessors * 50 + 50);

                elements
                    .forEach(computation => {
                        // console.log(Object.getOwnPropertyNames(computation));
                        // console.log(computation.DiagramComputingEntry);

                        if (computation.DiagramComputingEntry) {
                            const id = computation.DiagramComputingEntry.node;
                            const task = computation.DiagramComputingEntry.task;
                            const start = computation.DiagramComputingEntry.start;
                            const duration = computation.DiagramComputingEntry.duration;

                            const y = Number(id) * 50 + 75;
                            const startX = start * 10 + 85;
                            const endX = (start + duration) * 10 + 85;

                            ctx.moveTo(startX, y - 10);
                            ctx.lineTo(startX, y);

                            ctx.moveTo(endX, y - 10);
                            ctx.lineTo(endX, y);

                            ctx.fillText(task, (endX - startX) / 2 + startX, y - 10);
                        }
                    });

                ctx.stroke();
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

    #immutable-system-graph-holder {
        width: 100%;
        background-color: white;
        height: 60vh;
    }

    #ghant-diagram-connectivity {
        /*width: 1000px;*/
        /*width: 100%;*/
        /*height: 1000px;*/
        background-color: white;
    }

    .full-width {
        width: 100%;
    }

    .main-wrapper {
        width: 100%;
    }
</style>