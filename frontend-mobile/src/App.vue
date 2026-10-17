<template>
  <div class="mobile-layout">
    <router-view />
  </div>
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background-color: #f8fafc;
  color: #1e293b;
  -webkit-font-smoothing: antialiased;
}
.mobile-layout {
  max-width: 480px;
  min-height: 100vh;
  margin: 0 auto;
  background-color: #ffffff;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.05);
}
</style>
