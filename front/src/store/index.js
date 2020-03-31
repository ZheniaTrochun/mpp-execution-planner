import Vue from "vue";
import Vuex from "vuex";
import axios from "axios";

Vue.use(Vuex);

const persistState = state => {
    axios.put("http://localhost:9090/graphs/5e811a5c9bf9a107cab91861", {
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
