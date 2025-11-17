import { Fragment, useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { BoardProvider } from '@/context/BoardContext';
import { ColumnProvider } from '@/context/ColumnContext';
import { CardProvider } from '@/context/CardContext';
import DashboardPage from '@/pages/DashboardPage';
import LoginPage from '@/pages/LoginPage';
import { SignupPage } from '@/pages/SignupPage';
import OAuth2CallbackHandler from '@/pages/OAuth2CallbackHandler';
import BoardsPage from '@/pages/BoardsPage';
import BoardDetailPage from '@/pages/BoardDetailPage';
import ProfilePage from '@/pages/ProfilePage';
import { GlobalNavBar } from '@/components/GlobalNavBar';
import { Footer } from '@/components/Footer';
import { useAuth } from '@/context/AuthContext';
import { DialogProvider } from '@/hooks/useDialog';

const AUTH_CHROME_EXCLUDED_PATHS = ['/login', '/signup', '/oauth2/callback'];

const App = () => {
  const location = useLocation();
  const [displayLocation, setDisplayLocation] = useState(location);
  const [prevLocation, setPrevLocation] = useState(location);
  const [isAnimating, setIsAnimating] = useState(false);
  const { isAuthenticated } = useAuth();
  const shouldShowAppChrome = isAuthenticated && !AUTH_CHROME_EXCLUDED_PATHS.includes(location.pathname);

  useEffect(() => {
    if (location !== displayLocation) {
      setPrevLocation(displayLocation);
      setDisplayLocation(location);
      setIsAnimating(true);
    }
  }, [location, displayLocation]);

  const handleAnimationEnd = () => {
    setIsAnimating(false);
    setPrevLocation(location);
  };

  const routes = (
    <Fragment>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/oauth2/callback" element={<OAuth2CallbackHandler />} />
      <Route
        path="/"
        element={(
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/dashboard"
        element={(
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/boards"
        element={(
          <ProtectedRoute>
            <BoardsPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/boards/:workspaceId/:boardId"
        element={(
          <ProtectedRoute>
            <BoardDetailPage />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/profile"
        element={(
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        )}
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Fragment>
  );

  return (
    <DialogProvider>
      <BoardProvider>
        <ColumnProvider>
          <CardProvider>
            <div className="min-h-screen bg-gradient-pastel flex flex-col">
              {shouldShowAppChrome && <GlobalNavBar />}
              <div className="flex-1 relative min-h-0 overflow-hidden">
                {isAnimating && prevLocation && (
                  <div className="page-transition-wrapper fadeOut" onAnimationEnd={handleAnimationEnd}>
                    <Routes location={prevLocation}>
                      {routes}
                    </Routes>
                  </div>
                )}
                <div className={`page-transition-wrapper ${isAnimating ? 'fadeIn' : ''}`}>
                  <Routes location={displayLocation}>
                    {routes}
                  </Routes>
                </div>
              </div>
              {shouldShowAppChrome && <Footer />}
            </div>
          </CardProvider>
        </ColumnProvider>
      </BoardProvider>
    </DialogProvider>
  );
};

export default App;
