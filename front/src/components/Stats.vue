<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div class="main-wrapper">

        <div id="immutable-system-graph-holder"></div>

        <canvas class="chart-holder" id="chart1"></canvas>
        <canvas class="chart-holder" id="chart2"></canvas>
        <canvas class="chart-holder" id="chart3"></canvas>
        <canvas class="chart-holder" id="chart4"></canvas>


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
    import Chart from 'chart.js';

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
                    correlationStart: 0.2,
                    correlationLimit: 1,
                    correlationStep: 0.1
                }, {
                    timeout: 1000 * 60 * 5
                }
            )
                .then(resp => {
                    if (resp.status === 200) {
                        this.stats = resp.data;

                        this.drawChartBy(resp.data, 'chart1', 'Time', 20, this.calculateTimeCorrelationDataset);
                        this.drawChartBy(resp.data, 'chart2', 'Speedup', 20, this.calculateSpeedupCorrelationDataset);
                        this.drawChartBy(resp.data, 'chart3', 'Efficiency', 20, this.calculateEfficiencyCorrelationDataset);
                        this.drawChartBy(resp.data, 'chart4', 'Algorithm Efficiency', 20, this.calculateAlgorithmEfficiencyCorrelationDataset);
                    }
                });
        },
        created() {
            this.$vuetify.theme.dark = true;
        },
        computed: {
        },
        methods: {
            calculateTimeCorrelationDataset(data, queue, planning, size) {
                return data
                    .filter(x => x.queue === queue && x.planning === planning && x.size === size)
                    .map(x => {
                        return {
                            x: x.correlation,
                            y: x.time
                        };
                    });
            },
            calculateSpeedupCorrelationDataset(data, queue, planning, size) {
                return data
                    .filter(x => x.queue === queue && x.planning === planning && x.size === size)
                    .map(x => {
                        return {
                            x: x.correlation,
                            y: x.speedup
                        };
                    });
            },
            calculateEfficiencyCorrelationDataset(data, queue, planning, size) {
                return data
                    .filter(x => x.queue === queue && x.planning === planning && x.size === size)
                    .map(x => {
                        return {
                            x: x.correlation,
                            y: x.efficiency
                        };
                    });
            },
            calculateAlgorithmEfficiencyCorrelationDataset(data, queue, planning, size) {
                return data
                    .filter(x => x.queue === queue && x.planning === planning && x.size === size)
                    .map(x => {
                        return {
                            x: x.correlation,
                            y: x.algorithmEfficiency
                        };
                    });
            },
            drawChartBy(data, canvasId, yLabel, size, extractionFunc) {

                var names = ["queue 3 - planning 3", "queue 3 - planning 5", "queue 5 - planning 3", "queue 5 - planning 5", "queue 10 - planning 3", "queue 10 - planning 5"];

                const firstChartCtx = document.getElementById(canvasId).getContext('2d');
                const listOfPlanningAlgorithms = ['Algorithm 3', 'Algorithm 5'];
                const listOfQueueAlgorithms = ['Algorithm 3', 'Algorithm 5', 'Algorithm 10'];
                // const listOfSizes = [5, 10, 15, 20];

                let items = [];
                listOfQueueAlgorithms.forEach(queue => {
                    listOfPlanningAlgorithms.forEach(planning => {
                        items.push(extractionFunc(data, queue, planning, size).map(item => item.y));
                    });
                });

                const colors = ['red', 'blue', 'green', 'pink', 'black', 'brown'];

                const datasets = items.map((item, i) => {
                    return {
                        label: names[i],
                        backgroundColor: colors[i],
                        borderColor: colors[i],
                        data: item,
                        fill: false
                    }
                });

                const config = {
                    type: 'line',
                    data: {
                        labels: ['0.2', '0.3', '0.4', '0.5', '0.6', '0.7', '0.8', '0.9', '1.0'],
                        datasets: datasets
                    },
                    options: {
                        responsive: true,
                        title: {
                            display: true,
                            text: `${yLabel} by algorithm, size: ${size}`
                        },
                        tooltips: {
                            mode: 'index',
                            intersect: false,
                        },
                        hover: {
                            mode: 'nearest',
                            intersect: true
                        },
                        scales: {
                            xAxes: [{
                                display: true,
                                scaleLabel: {
                                    display: true,
                                    labelString: 'correlation'
                                }
                            }],
                            yAxes: [{
                                display: true,
                                scaleLabel: {
                                    display: true,
                                    labelString: yLabel
                                }
                            }]
                        }
                    }
                };

                return new Chart(firstChartCtx, config);
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

    .chart-holder {
        background-color: white;
    }
</style>
