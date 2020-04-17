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
                        <v-btn color="v-btn--outlined deep-purple add-button" @click="fileLoadDialog = true">Load from file</v-btn>
                    </div>
                </v-card-actions>
            </v-card>

            <v-dialog v-model="fileLoadDialog" max-width="650px">
                <v-card>
                    <v-card-title>
                        <span class="headline">Choose file</span>
                    </v-card-title>
                    <v-card-text>
                        <template>
                            <v-file-input v-model="file" show-size label="File input"></v-file-input>
                        </template>
                    </v-card-text>
                    <v-card-actions>
                        <v-spacer></v-spacer>
                        <v-btn color="v-btn--outlined deep-purple" @click="fileLoadDialog = false">Cancel</v-btn>
                        <v-btn color="v-btn--outlined deep-purple" @click="loadTaskFromFile()">Load</v-btn>
                    </v-card-actions>
                </v-card>
            </v-dialog>
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
                dialog: true,
                fileLoadDialog: false,
                file: null
            }
        },
        mounted() {

            axios.get('https://cluster-planner-server.herokuapp.com/task/list')
                .then(resp => {
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

                this.postTaskName(name, task => this.selectTask(task));
            },
            postTaskName(name, cb) {
                axios.post("https://cluster-planner-server.herokuapp.com/task", { name: name }, {
                    headers: {
                        'Content-Type': 'application/json',
                    }
                }).then(resp => {
                    if (resp.status === 200 || resp.status === 201) {
                        cb({name: name, id: resp.data.id});
                    }
                });
            },
            loadTaskFromFile() {
                console.log(this.file);

                const reader = new FileReader();
                reader.addEventListener('load', readEvent => this.onReadDataFromFile(readEvent));
                reader.readAsText(this.file);
            },
            onReadDataFromFile(readEvent) {
                const data = JSON.parse(readEvent.target.result);

                const taskName = data.task.name;
                const taskGraph = data.taskGraph;
                const systemGraph = data.systemGraph;

                // console.log(systemGraph);

                this.postTaskName(taskName, task => {
                    store.commit('setSelectedTask', task);
                    store.commit('persistTaskGraph', taskGraph);
                    store.commit('persistSystemGraph', systemGraph);

                   setTimeout(() => this.$router.push('/graph/task'), 500);

                   this.dialog = false;
                   this.fileLoadDialog = false;
                });
            },
            deleteSelected(task) {
                axios.delete(`https://cluster-planner-server.herokuapp.com/task/${task.id}`)
                    .then(resp => {
                        if (resp.status === 200) {
                            this.tasks = this.tasks.filter(t => t.id !== task.id);
                        }
                    });
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
