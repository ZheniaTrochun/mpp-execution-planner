<template>
  <v-app id="inspire">
    <v-navigation-drawer v-model="drawer" app clipped>
      <v-list dense>
        <router-link to="/">
          <v-list-item link>
              <v-list-item-action>
                  <v-icon>mdi-home</v-icon>
              </v-list-item-action>
              <v-list-item-content>
                  <v-list-item-title>Choose task</v-list-item-title>
              </v-list-item-content>
          </v-list-item>
        </router-link>
        <router-link to="/graph/task">
          <v-list-item link>
            <v-list-item-action>
              <v-icon>mdi-graph-outline</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>Graph of task</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </router-link>
        <router-link to="/graph/system">
          <v-list-item link>
            <v-list-item-action>
              <v-icon>mdi-graph</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>Graph of system</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </router-link>
        <router-link to="/modeling">
          <v-list-item link>
            <v-list-item-action>
              <v-icon>mdi-view-dashboard</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>Modeling</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </router-link>
        <router-link to="/stats">
          <v-list-item link>
            <v-list-item-action>
              <v-icon>mdi-chart-bell-curve</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>Statistics</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </router-link>
        <v-list-item link>
          <v-list-item-action>
            <v-icon>mdi-settings</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>Settings</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link class="deep-purple" @click="saveEverythingToFile()">
          <v-list-item-action>
              <v-icon>mdi-cloud-download</v-icon>
          </v-list-item-action>
          <v-list-item-content>
              <v-list-item-title>Save to file</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-navigation-drawer>

    <v-app-bar app clipped-left>
      <v-app-bar-nav-icon @click.stop="drawer = !drawer" />
      <v-toolbar-title>PZKS-2 Labs</v-toolbar-title>
    </v-app-bar>

    <v-content>
      <v-container class="fill-height" fluid>
        <router-view />
      </v-container>
    </v-content>

    <v-footer app>
      <span>&copy; 2020, Yevhenii Trochun</span>
    </v-footer>
  </v-app>
</template>

<script>
    import store from './store';

    export default {
        props: {
            source: String
        },
        data: () => ({
            drawer: null
        }),
        created() {
            this.$vuetify.theme.dark = true;
        },
        methods: {
            saveEverythingToFile() {

                const task = store.state.selectedTask;

                const data = {
                    task: store.state.selectedTask,
                    taskGraph: store.state.taskGraph,
                    systemGraph: store.state.systemGraph
                };

                const fileName = `pzks2-task-${task.name}.json`;

                var a = document.createElement("a");
                document.body.appendChild(a);
                a.style.display = "none";

                let json = JSON.stringify(data, null, '\t');
                let blob = new Blob([json], { type: "octet/stream" });
                let url = window.URL.createObjectURL(blob);

                a.href = url;
                a.download = fileName;
                a.click();
                window.URL.revokeObjectURL(url);
            }
        }
    };
</script>
