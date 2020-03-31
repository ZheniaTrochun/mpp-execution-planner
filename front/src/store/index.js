import Vue from "vue";
import Vuex from "vuex";
import axios from "axios";

Vue.use(Vuex);

const persistState = state => {
    axios.put("http://localhost:9090/graphs/5e83c55bf35e75051b99db3a", {
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
      systemGraph: []
  },
  mutations: {
      setTaskGraph(state, data) {
          state.taskGraph = data;

          persistState(state)
      },
      setSystemGraph(state, data) {
          state.systemGraph = data;

          persistState(state)
      }
  },
  actions: {},
  modules: {}
});
