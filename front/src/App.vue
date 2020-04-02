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
      </v-list>
    </v-navigation-drawer>

    <v-app-bar app clipped-left>
      <v-app-bar-nav-icon @click.stop="drawer = !drawer" />
      <v-toolbar-title>Yevhenii Trochun IP-93mn</v-toolbar-title>
    </v-app-bar>

    <v-content>
      <v-container class="fill-height" fluid>
        <router-view />
      </v-container>
    </v-content>

    <v-footer app>
      <span>&copy; 2019</span>
    </v-footer>
  </v-app>
</template>

<script>
    export default {
      props: {
        source: String
      },
      data: () => ({
        drawer: null
      }),
      created() {
        this.$vuetify.theme.dark = true;
      }
    };
</script>
