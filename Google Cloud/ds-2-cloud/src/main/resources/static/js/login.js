let firebaseConfig;
if (location.hostname === "localhost") {
    firebaseConfig = {
        apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
        projectId: "demo-distributed-systems-kul",
    }
} else {
    // TODO: (level 2) replace with your own configuration
    firebaseConfig = {  apiKey: "AIzaSyAHT8-KjMG4v9gmpqveZQWvli-25d4MkWU",
                          authDomain: "ds-booking-system.firebaseapp.com",
                          projectId: "ds-booking-system",
                          storageBucket: "ds-booking-system.appspot.com",
                          messagingSenderId: "72467283480",
                          appId: "1:72467283480:web:74d5a857c156be0375cd6d"};
}
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
if (location.hostname === "localhost") {
    auth.useEmulator("http://localhost:8082");
}
const ui = new firebaseui.auth.AuthUI(auth);

ui.start('#firebaseui-auth-container', {
    signInOptions: [
        firebase.auth.EmailAuthProvider.PROVIDER_ID
    ],
    callbacks: {
        signInSuccessWithAuthResult: function (authResult, redirectUrl) {
            auth.currentUser.getIdToken(true)
                .then(async (idToken) => {
                    await fetch("/authenticate", {
                        method: "POST",
                        body: idToken,
                        headers: {
                            "Content-Type": "plain/text"
                        }
                    });
                    location.assign("/");
                });
            return false;
        },
    },
});
