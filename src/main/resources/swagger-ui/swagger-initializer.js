window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle(
  {
    urls: [
          {url: "./yaml/repo.yaml", name: "Repositories"},
          {url: "./yaml/token-gen.yaml", name: "Auth tokens"},
          {url: "./yaml/users.yaml", name: "Users"},
          {url: "./yaml/roles.yaml", name: "Roles"},
          {url: "./yaml/settings.yaml", name: "Settings"},
        ],
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  }
  );

  //</editor-fold>
};
