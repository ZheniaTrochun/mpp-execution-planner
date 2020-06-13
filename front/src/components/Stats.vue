<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div class="main-wrapper">

        <div id="immutable-system-graph-holder"></div>

        <div class="holder">
            <div id="chart1-holder-1235"></div>
        </div>

        <canvas class="chart-holder" id="chart2"></canvas>
        <canvas class="chart-holder" id="chart3"></canvas>


        <v-card flat tile>
            <v-simple-table>
                <template v-slot:default>
                    <thead>
                    <tr>
                        <th class="text-left">id</th>
                        <th class="text-left">Task size</th>
                        <th class="text-left">Task correlation</th>
                        <th class="text-left">Queue algorithm</th>
                        <th class="text-left">Planning algorithm</th>
                        <th class="text-left">Time</th>
                        <th class="text-left">Speedup</th>
                        <th class="text-left">Efficiency</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="(item, index) in stats" v-bind:key="index">
                        <td>{{ index + 1 }}</td>
                        <td>{{ item.size }}</td>
                        <td>{{ item.correlation }}</td>
                        <td>{{ item.queue }}</td>
                        <td>{{ item.planning }}</td>
                        <td>{{ item.time }}</td>
                        <td>{{ item.speedup }}</td>
                        <td>{{ item.efficiency }}</td>
                    </tr>
                    </tbody>
                </template>
            </v-simple-table>
        </v-card>
    </div>
</template>

<script>
    import cytoscape from "cytoscape";
    import store from "../store";
    import axios from 'axios';
    // import Chart from 'chart.js';
    // import vis from 'vis'
    import {DataSet, Graph2d} from 'vis';

    let systemGraph;

    export default {
        name: "Stats",
        data () {
            return {
                tab: null,
                tab2: null,
                icons: false,
                centered: false,
                grow: true,
                vertical: false,
                prevIcon: true,
                nextIcon: true,
                right: false,

                stats: []
            }
        },
        mounted() {
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

                        resp.data.systemGraph.forEach(x => {
                            systemGraph.add(x);
                        });

                        systemGraph.autolock(true);
                    }
                });

            axios.post(
                `https://cluster-planner-server.herokuapp.com/stats/${this.task.id}`,
                {
                    maxSizeMultiplier: 4,
                    correlationStart: 0.3,
                    correlationLimit: 1,
                    correlationStep: 0.1
                }, {
                    timeout: 1000 * 60 * 5
                }
            )
                .then(resp => {
                    if (resp.status === 200) {
                        this.stats = resp.data;

                        var names = ["Algo3-Algo3", "Algo5-Algo3", "Algo10-Algo3", "Algo3-Algo5", "Algo5-Algo5", "Algo10-Algo5"];
                        var groups = new DataSet();
                        names.forEach((name, i) => {
                            groups.add({
                                id: i,
                                content: name,
                                options: {
                                    drawPoints: {
                                        style: "square", // square, circle
                                    },
                                    shaded: {
                                        orientation: "bottom", // top, bottom
                                    },
                                },
                            });
                        });

                        var container = document.getElementById("chart1-holder-1235");

                        // const firstChartCtx = document.getElementById("chart1").getContext('2d');
                        const listOfPlanningAlgorithms = ['Algorithm 3', 'Algorithm 5'];
                        const listOfQueueAlgorithms = ['Algorithm 3', 'Algorithm 5', 'Algorithm 10'];
                        // const listOfSizes = [5, 10, 15, 20];

                        let items = [];
                        // const data = listOfSizes.map(size => {
                            listOfQueueAlgorithms.forEach((queue, i) => {
                                listOfPlanningAlgorithms.forEach((planning, j) => {
                                    const data = this.calulateCorrelationDataset(resp.data, queue, planning, 15);
                                    data.forEach(item => {
                                        items.push({
                                            x: item.x,
                                            y: item.y,
                                            group: (j * listOfPlanningAlgorithms.length) + i
                                        })
                                    });
                                });
                            });

                        console.log(items);
                        // var dataset = new vis.DataSet(items);
                        var dataset = new DataSet(items);
                        var options = {
                            defaultGroup: "ungrouped",
                            legend: true,
                            start: 0,
                            end: 1,
                        };
                        // new vis.Graph2d(container, dataset, groups, options);
                        new Graph2d(container, dataset, groups, options);

                        // });


                    }
                });
        },
        created() {
            this.$vuetify.theme.dark = true;
        },
        computed: {
        },
        methods: {
            calulateCorrelationDataset(data, queue, planning, size) {
                return data
                    .filter(x => x.queue === queue && x.planning === planning && x.size === size)
                    .map(x => {
                        return {
                            x: x.correlation,
                            y: x.time
                        };
                    });
            }
        }
    };
</script>

<style scoped>
    #immutable-system-graph-holder {
        width: 100%;
        background-color: white;
        height: 60vh;
    }

    .holder {
        width: 100%;
        /*background-color: white;*/
        height: 60vh;
        margin-top: 25px;
        position: relative;
    }

    .main-wrapper {
        width: 100%;
    }
</style>
