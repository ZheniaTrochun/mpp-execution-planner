import Vue from "vue";
import VueRouter from "vue-router";
import SystemGraph from "../components/SystemGraph.vue";
import TaskGraph from "../components/TaskGraph.vue";
import ChooseTask from "../components/ChooseTask.vue";
import Modeling from "../components/Modeling.vue";

Vue.use(VueRouter);

const routes = [
  {
    path: "/graph/system",
    name: "SystemGraph",
    component: SystemGraph
  },
  {
    path: "/graph/task",
    name: "TaskGraph",
    component: TaskGraph
  },
  {
    path: "/modeling",
    name: "Modeling",
    component: Modeling
  },
  {
    path: "/",
    name: "ChooseTask",
    component: ChooseTask
  }
];

const router = new VueRouter({
  routes
});

export default router;
