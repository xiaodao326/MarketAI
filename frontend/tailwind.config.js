/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#ebf4fd',
          100: '#d6e9fb',
          200: '#add3f7',
          300: '#84bdf3',
          400: '#5ba7ef',
          500: '#378ADD',
          600: '#2c6eb1',
          700: '#215385',
          800: '#163758',
          900: '#0b1c2c',
        },
      },
    },
  },
  plugins: [],
}
