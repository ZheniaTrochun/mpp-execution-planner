<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
    <div class="main-wrapper">

        <button v-on:click="drawDiagram()">draw</button>

        <div id="visualization"></div>

        <canvas id="canvas" style="display: block; height: 388px; width: 776px;" width="1552" height="776" class="chartjs-render-monitor"></canvas>
    </div>
</template>

<script>
    import Chart from 'chart.js'

    export default {
        name: "Test",
        data () {
            return {
                seed: 1243
            }
        },
        mounted() {

        },
        created() {
            // this.$vuetify.theme.dark = true;
        },
        computed: {
        },
        methods: {
            drawDiagram() {

                // var MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
                const firstData = [
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor()
                ];

                const secondData = [
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor(),
                    this.randomScalingFactor()
                ];

                console.log(firstData);
                console.log(secondData);

                var config = {
                    type: 'line',
                    data: {
                        labels: ['January', 'February', 'March', 'April', 'May', 'June', 'July'],
                        datasets: [{
                            label: 'My First dataset',
                            backgroundColor: "red",
                            borderColor: "red",
                            data: firstData,
                            fill: false
                        }, {
                            label: 'My Second dataset',
                            fill: false,
                            backgroundColor: "blue",
                            borderColor: "blue",
                            data: secondData
                        }]
                    },
                    options: {
                        responsive: true,
                        title: {
                            display: true,
                            text: 'Chart.js Line Chart'
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
                                    labelString: 'Month'
                                }
                            }],
                            yAxes: [{
                                display: true,
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Value'
                                }
                            }]
                        }
                    }
                };

                // window.onload = function() {
                    var ctx = document.getElementById('canvas').getContext('2d');
                    window.myLine = new Chart(ctx, config);
                // };

                // document.getElementById('randomizeData').addEventListener('click', function() {
                //     config.data.datasets.forEach(function(dataset) {
                //         dataset.data = dataset.data.map(function() {
                //             return randomScalingFactor();
                //         });
                //
                //     });
                //
                //     window.myLine.update();
                // });

                // var colorNames = Object.keys(window.chartColors);

            },
            rand(min, max) {
                min = min === undefined ? 0 : min;
                max = max === undefined ? 1 : max;
                this.seed = (this.seed * 9301 + 49297) % 233280;
                return min + (this.seed / 233280) * (max - min);
            },
            randomScalingFactor() {
                return Math.round(this.rand(-100, 100));
            }
        }
    };
</script>

<style scoped>
    .main-wrapper {
        width: 100%;
    }

    .chartjs-render-monitor {
        background-color: #fff;
    }
</style>
