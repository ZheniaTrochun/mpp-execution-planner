<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div class="main-wrapper">

        <div id="immutable-system-graph-holder"></div>

        <v-card flat tile>
            <v-simple-table>
                <template v-slot:default>
                    <thead>
                    <tr>
                        <th class="text-left">Task size</th>
                        <th class="text-left">Queue algorithm</th>
                        <th class="text-left">Planning algorithm</th>
                        <th class="text-left">Time</th>
                        <th class="text-left">Speedup</th>
                        <th class="text-left">Efficiency</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="(item, index) in stats" v-bind:key="index">
                        <td>{{ item.size }}</td>
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
                    connectivityStart: 0,
                    connectivityLimit: 1,
                    connectivityStep: 0.1
                }
            )
                .then(resp => {
                    if (resp.status === 200) {
                        this.stats = resp.data
                    }
                });
        },
        created() {
            this.$vuetify.theme.dark = true;
        },
        computed: {
        },
        methods: {
        }
    };
</script>

<style scoped>
    #immutable-system-graph-holder {
        width: 100%;
        background-color: white;
        height: 60vh;
        margin-top: -215px;
    }

    .main-wrapper {
        width: 100%;
    }
</style>
