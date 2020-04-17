import Vue from "vue";
import Vuex from "vuex";
import axios from "axios";

Vue.use(Vuex);

const persistState = state => {
    axios.put(`https://cluster-planner-server.herokuapp.com/graphs/${state.selectedTask.id}`, {
        id: "",
        taskGraph: state.taskGraph,
        systemGraph: state.systemGraph
    }, {
        headers: {
            'Content-Type': 'application/json',
        }
    })
};

export default new Vuex.Store({
  state: {
      taskGraph: [],
      systemGraph: [],
      selectedTask: {}
  },
  mutations: {
      persistTaskGraph(state, data) {
          state.taskGraph = data;

          persistState(state);
      },
      persistSystemGraph(state, data) {
          state.systemGraph = data;

          persistState(state)
      },
      setSelectedTask(state, data) {
          state.selectedTask = data;
      },
      initState(state, data) {
          state.systemGraph = data.systemGraph;
          state.taskGraph = data.taskGraph;
      }
  },
  actions: {},
  modules: {}
});
