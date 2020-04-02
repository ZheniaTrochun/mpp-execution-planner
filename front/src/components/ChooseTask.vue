<template>
    <v-row justify="center">
        <v-dialog v-model="dialog" persistent max-width="80%">

            <v-card height="80vh">
                <v-card-title>
                    <span class="headline">Choose task</span>
                </v-card-title>
                <v-card-text>
                    <v-card v-for="task in tasks" :key="task.id" color="#1F7087" class="mt-4">
                        <v-card-text class="title centered-text" @click="selectTask(task)">
                            {{ task.name }}
                        </v-card-text>
                        <v-btn class="v-btn--outlined red darken-4 remove-button" @click="deleteSelected(task)">X</v-btn>
                    </v-card>
                </v-card-text>
                <v-card-actions>
                    <div class="mx-auto">
                        <v-btn color="v-btn--outlined deep-purple add-button" @click="createNewTask()">Create new</v-btn>
                        <v-btn color="v-btn--outlined deep-purple add-button" @click="loadTaskFromFile()">Load from file</v-btn>
                    </div>
                </v-card-actions>
            </v-card>
        </v-dialog>
    </v-row>
</template>

<script>
    import store from "../store";
    import axios from "axios";

    export default {
        name: "ChooseTask",
        data() {
            return {
                tasks: [],
                dialog: true
            }
        },
        mounted() {

            axios.get('https://cluster-planner-server.herokuapp.com/task/list', {
                headers: {
                    'Content-Type': 'application/json',
                }
            }).then(resp => {
                if (resp.status === 200) {
                    this.tasks = resp.data;
                }
            });
        },
        created() {
            this.$vuetify.theme.dark = true;
        },
        computed: { },
        methods: {
            selectTask(task) {
                store.commit("setSelectedTask", task);

                this.$router.push("/graph/task");
            },
            createNewTask() {
                const name = prompt("Enter name of new task");

                axios.post("https://cluster-planner-server.herokuapp.com/task", { name: name }, {
                    headers: {
                        'Content-Type': 'application/json',
                    }
                }).then(resp => {
                    if (resp.status === 200 || resp.status === 201) {
                        this.selectTask({name: name, id: resp.data.id});
                    }
                });
            },
            loadTaskFromFile() {

            },
            deleteSelected(task) {

            },
            deleteNode() {
            },
            afterCreated() {
            }
        }
    };
</script>

<style>
    .centered-text {
        text-align: center;
    }

    .centered-text:hover {
        cursor: pointer;
    }

    .add-button {
        margin: 10px;
        width: 150px;
    }

    .remove-button {
        position: absolute;
        right: 25px;
        top: 14px;
    }
</style>
